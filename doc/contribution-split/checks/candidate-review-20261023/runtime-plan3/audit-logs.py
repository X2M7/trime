"""Read an already stopped #2187 controller log. No ADB, VM or source changes."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
from runtime_common import ROOT


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    run = args.run.resolve()
    out = args.out.resolve()
    assert run.is_relative_to(ROOT) and out.is_relative_to(ROOT) and not out.exists()
    completion = json.loads((run / 'completion.json').read_bytes())
    source = run / 'device-logcat.txt'
    before = source.stat()
    out.mkdir()
    pattern = re.compile(rb'^(\d\d-\d\d) (\d\d:\d\d:\d\d\.\d+)\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+([^:]+): ?(.*)')
    counts = Counter()
    events = []
    starts = []
    digest = hashlib.sha256()
    position = 0
    with source.open('rb') as stream, (out / 'all-WEF.raw.txt').open('wb') as warnings:
        for number, line in enumerate(stream, 1):
            digest.update(line)
            match = pattern.match(line)
            if match:
                level = match[5].decode()
                tag = match[6].decode('utf-8', 'replace').strip()
                message = match[7].decode('utf-8', 'replace')
                counts[level] += 1
                if level in 'WEF':
                    warnings.write(line)
                kinds = []
                if level == 'F':
                    kinds.append('F-priority-retained-not-automatically-process-fatal')
                if tag == 'AndroidRuntime' and ('FATAL EXCEPTION' in message or 'OutOfMemoryError' in message):
                    kinds.append('runtime-fatal-or-oom')
                if tag in ('libc', 'DEBUG') and 'Fatal signal ' in message:
                    kinds.append('native-fatal')
                if tag in ('am_anr', 'am_crash') or (tag == 'ActivityManager' and message.startswith('ANR in ')):
                    kinds.append('ANR-or-crash-record')
                if tag in ('lowmemorykiller', 'lmkd') and re.search(r'\b[kK]ill', message):
                    kinds.append('LMK')
                row = dict(line=number, byte_offset=position, pid=int(match[3]), tag=tag,
                           text=line.decode('utf-8', 'replace'))
                if tag == 'am_proc_start' and re.search(r',com\.osfans\.trime\.debug(?:,|\])', message):
                    starts.append(row)
                if kinds:
                    events.append(dict(kinds=kinds, **row))
            position += len(line)
    after = source.stat()
    assert (before.st_ino, before.st_dev, before.st_size) == (after.st_ino, after.st_dev, position)
    (out / 'audit.json').write_text(json.dumps(dict(
        source=dict(path=str(source), bytes=position, inode=before.st_ino,
                    device=before.st_dev, sha256=digest.hexdigest()),
        priority_counts=dict(counts), critical_event_records=events,
        actual_Trime_process_start_records=starts, controller_completion=completion,
        scope='Metadata/tag classification of this collected source only. No automatic family exemption, '
              'no global zero-warning claim; no process/native conclusion from hardcoded probe booleans.',
        physical_device='NOT_RUN', independent_visual_qualification=False), ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
