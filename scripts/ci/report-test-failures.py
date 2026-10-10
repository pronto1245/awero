#!/usr/bin/env python3
"""Surface failing test names and messages as GitHub Actions error annotations.

Annotations are visible on the PR checks page and through the check-run
annotations API, so a failure can be diagnosed without downloading the full
job log or test-report artifacts.

Usage:
  report-test-failures.py --junit DIR   # JUnit XML (Android instrumentation)
  report-test-failures.py --log FILE    # xcodebuild test output (iOS)
"""

import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

MAX_ANNOTATIONS = 10  # GitHub keeps at most 10 error annotations per step
MAX_MESSAGE = 900


def escape(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "").replace("\n", "%0A")


def emit(title: str, message: str) -> None:
    message = message.strip()[:MAX_MESSAGE] or "(no failure message)"
    print(f"::error title={escape(title)[:200]}::{escape(message)}")


def from_junit(directory: pathlib.Path) -> list[tuple[str, str]]:
    failures = []
    for path in sorted(directory.rglob("*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for case in root.iter("testcase"):
            for kind in ("failure", "error"):
                node = case.find(kind)
                if node is None:
                    continue
                name = f"{case.get('classname', '?')}.{case.get('name', '?')}"
                text = (node.get("message") or "") + "\n" + (node.text or "")
                # Keep the assertion line and the first app frames, drop framework noise.
                lines = [l for l in text.splitlines() if l.strip()]
                kept = [l for l in lines if "app.awero" in l or not l.strip().startswith("at ")]
                failures.append((name, "\n".join(kept[:14])))
    return failures


XCODE_FAILURE = re.compile(r"(?P<loc>[^\s:]+\.swift:\d+): error: (?P<test>-\[[^\]]+\]) : (?P<msg>.*)")
XCODE_FAILED_CASE = re.compile(r"Test Case '(?P<test>-\[[^\]]+\])' failed")


def from_log(path: pathlib.Path) -> list[tuple[str, str]]:
    failures, failed_cases, seen = [], [], set()
    if not path.exists():
        return [("xcodebuild", f"log not found: {path}")]
    for line in path.read_text(errors="replace").splitlines():
        match = XCODE_FAILURE.search(line)
        if match:
            key = (match["test"], match["msg"])
            if key not in seen:
                seen.add(key)
                failures.append((match["test"], f"{match['loc']}\n{match['msg']}"))
            continue
        match = XCODE_FAILED_CASE.search(line)
        if match:
            failed_cases.append(match["test"])
    reported = {test for test, _ in failures}
    for test in failed_cases:
        if test not in reported:
            reported.add(test)
            failures.append((test, "failed without an assertion message (crash or timeout?)"))
    if not failures:
        tail = [l for l in path.read_text(errors="replace").splitlines() if "error" in l.lower()][-6:]
        failures.append(("xcodebuild", "\n".join(tail) or "no test failures found in log"))
    return failures


def main() -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--junit", type=pathlib.Path)
    group.add_argument("--log", type=pathlib.Path)
    args = parser.parse_args()

    failures = from_junit(args.junit) if args.junit else from_log(args.log)
    if not failures:
        print("No failing test cases found in reports.")
        return 0
    print(f"{len(failures)} failing test(s); annotating the first {MAX_ANNOTATIONS}.")
    for title, message in failures[:MAX_ANNOTATIONS]:
        emit(title, message)
    for title, _ in failures[MAX_ANNOTATIONS:]:
        print(f"also failed: {title}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
