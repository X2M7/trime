import java.io.PrintWriter;
import java.nio.file.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public class DiagnosticJUnit {
  public static void main(String[] args) {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
      .selectors(selectClass("com.osfans.trime.util.CancellationCleanupDiagnostic"))
      .configurationParameter("kotest.framework.classpath.scanning.autoscan.disable", "true")
      .configurationParameter("kotest.framework.classpath.scanning.config.disable", "true")
      .build();
    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    Launcher launcher = LauncherFactory.create();
    launcher.registerTestExecutionListeners(summary, new TestExecutionListener() {
      public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (id.isTest()) System.out.println(result.getStatus() + " " + id.getDisplayName());
      }
    });
    launcher.execute(request);
    summary.getSummary().printTo(new PrintWriter(System.out, true));
    summary.getSummary().printFailuresTo(new PrintWriter(System.out, true));
    if (summary.getSummary().getTestsFoundCount() == 0 || summary.getSummary().getTotalFailureCount() != 0)
      System.exit(1);
  }
}
