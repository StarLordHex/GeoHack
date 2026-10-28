"""Engineer: report failures and run regression suite. Does not auto-edit or deploy."""
import subprocess
import sys
from ai_lab.breaker import run

def main():
    problems = run()
    print(f"Breaker findings: {len(problems)}")
    for problem in problems: print("BUG:", problem)
    result = subprocess.run([sys.executable, "-m", "unittest", "discover", "-s", "tests", "-v"])
    if problems or result.returncode:
        print("ENGINEER: Review failures and propose a patch for human approval.")
        return 1
    print("ENGINEER: Checks passed; human approval still required before deployment.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
