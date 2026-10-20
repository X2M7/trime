import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.AndroidSourceDirectorySet;
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet;
import com.android.build.gradle.internal.api.artifact.SourceArtifactType;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testfixtures.internal.ProjectBuilderImpl;

public final class SourceDirectoryComparison {
    private static int assertions;
    private static final List<Project> projects = new ArrayList<>();
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        root.mkdirs();
        try {
            compareRawApi(new File(root, "relative project with spaces"));
            compareAndroidPlugin(new File(root, "actual android plugin"));
        } finally {
            Collections.reverse(projects);
            for (Project p : projects) {
                ProjectBuilderImpl.stop(p);
                System.out.println("CLEANUP stopped ProjectBuilder root " + p.getProjectDir());
            }
        }
        System.out.println("FIXTURE_PASS assertions=" + assertions + " cleaned-projects=" + projects.size());
        // ProjectBuilder/AGP global executor threads outlive per-project teardown in a standalone JVM.
        // Reach successful process exit only after every assertion and both teardown calls return.
        System.exit(0);
    }
    private static Project project(File dir) {
        dir.mkdirs();
        Project p = ProjectBuilder.builder().withProjectDir(dir).build();
        projects.add(p);
        return p;
    }
    private static DefaultAndroidSourceDirectorySet directories(Project p) {
        return new DefaultAndroidSourceDirectorySet("main", "jniLibs", p, SourceArtifactType.JNI_LIBS.INSTANCE);
    }
    private static void createFiles(File root) throws Exception {
        new File(root, "src/main/jniLibs/arm64-v8a").mkdirs();
        new File(root, "custom native libs/arm64-v8a").mkdirs();
        new File(root, "prebuilt/arm64-v8a").mkdirs();
        Files.write(new File(root, "src/main/jniLibs/arm64-v8a/libdefault.so").toPath(), new byte[] {1});
        Files.write(new File(root, "custom native libs/arm64-v8a/libcustom.so").toPath(), new byte[] {2});
        Files.write(new File(root, "prebuilt/arm64-v8a/libprebuilt.so").toPath(), new byte[] {3});
    }
    private static void compareRawApi(File root) throws Exception {
        Project p = project(root);
        DefaultAndroidSourceDirectorySet before = directories(p);
        DefaultAndroidSourceDirectorySet after = directories(p);
        before.getDirectories().add("src/main/jniLibs");
        after.getDirectories().add("src/main/jniLibs");
        before.getDirectories().add("custom native libs");
        after.getDirectories().add("custom native libs");
        createFiles(root);
        check(delegates(before).isEmpty() && delegates(after).isEmpty(), "direct fixtures start without late delegates");
        before.srcDirs(Collections.singleton("prebuilt"));
        after.getDirectories().add("prebuilt");
        check(raw(before).equals(raw(after)), "same raw entries and insertion order");
        check(raw(before).contains("src/main/jniLibs"), "default source is retained");
        check(raw(before).contains("custom native libs"), "custom source is retained");
        check(raw(before).get(raw(before).size() - 1).equals("prebuilt"), "relative prebuilt string appended unchanged");
        check(before.getSrcDirs().equals(after.getSrcDirs()), "same resolved directory sets");
        check(new ArrayList<>(before.getSrcDirs()).equals(new ArrayList<>(after.getSrcDirs())), "same pinned-implementation resolved order");
        Set<File> expected = new LinkedHashSet<>(Arrays.asList(
            new File(root, "src/main/jniLibs"), new File(root, "custom native libs"), new File(root, "prebuilt")));
        check(before.getSrcDirs().equals(expected), "resolved paths stay relative to project with spaces");
        check(before.getSourceFiles().getFiles().equals(after.getSourceFiles().getFiles()), "same actual source file set");
        check(before.getSourceFiles().getFiles().size() == 3, "default/custom/prebuilt files remain present");
        before.srcDirs(Collections.singleton("prebuilt"));
        after.getDirectories().add("prebuilt");
        check(raw(before).equals(raw(after)), "duplicate additions preserve existing old/new behavior");
        System.out.println("CASE ordinary raw=" + raw(before));
        System.out.println("CASE ordinary resolved=" + before.getSrcDirs());
    }
    private static void compareAndroidPlugin(File root) throws Exception {
        Project p = project(root);
        p.getPluginManager().apply("com.android.application");
        ApplicationExtension android = p.getExtensions().getByType(ApplicationExtension.class);
        AndroidSourceDirectorySet actual = android.getSourceSets().getByName("main").getJniLibs();
        check(actual instanceof DefaultAndroidSourceDirectorySet, "real Android plugin uses examined AGP implementation");
        DefaultAndroidSourceDirectorySet source = (DefaultAndroidSourceDirectorySet) actual;
        check(delegates(source).isEmpty(), "actual Android plugin early apply phase has no late delegates");
        check(source.getSrcDirs().contains(new File(root, "src/main/jniLibs")), "real Android main default JNI directory exists before addition");
        source.getDirectories().add("custom native libs");
        createFiles(root);
        check(p.file("prebuilt").exists(), "actual plugin fixture meets unchanged prebuilt existence condition");
        List<Object> initial = raw(source);
        source.srcDirs(Collections.singleton("prebuilt"));
        List<Object> oldRaw = raw(source);
        List<File> oldDirectories = new ArrayList<>(source.getSrcDirs());
        Set<File> oldFiles = new LinkedHashSet<>(source.getSourceFiles().getFiles());
        source.getDirectories().remove("prebuilt");
        check(raw(source).equals(initial), "fixture restores identical early source state");
        source.getDirectories().add("prebuilt");
        check(raw(source).equals(oldRaw), "old and new additions match in actual early Android DSL");
        check(new ArrayList<>(source.getSrcDirs()).equals(oldDirectories), "actual plugin resolved directories and order agree");
        check(source.getSourceFiles().getFiles().equals(oldFiles), "actual plugin source files agree");
        check(oldFiles.size() == 3, "actual plugin retains default/custom/prebuilt files");
        check(delegates(source).isEmpty(), "addition itself does not finalize or create variants");
        System.out.println("CASE actual-plugin early-delegates=" + delegates(source).size() + " raw=" + raw(source));
    }
    private static List<?> delegates(DefaultAndroidSourceDirectorySet dirs) throws Exception {
        Field field = DefaultAndroidSourceDirectorySet.class.getDeclaredField("lateAdditionsDelegates");
        field.setAccessible(true);
        return (List<?>) field.get(dirs);
    }
    private static List<Object> raw(DefaultAndroidSourceDirectorySet dirs) throws Exception {
        Field field = DefaultAndroidSourceDirectorySet.class.getDeclaredField("source");
        field.setAccessible(true);
        return new ArrayList<>((List<?>) field.get(dirs));
    }
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        assertions++;
        System.out.println("PASS " + description);
    }
}
