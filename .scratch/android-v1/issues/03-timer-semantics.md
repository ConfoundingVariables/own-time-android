# Timer semantics

Type: grilling
Status: open
Blocked by:

## Question

Exactly how do sessions and budgets behave? Settle: pause vs stop (are they the same?); what happens when a budget hits zero (does the session keep running into overtime, stop automatically, or stop when the alarm is dismissed; how is overtime shown); starting another priority while one runs; a session running across midnight; time-zone changes and manual clock changes; budget edits mid-day and mid-session; log adjustments (range, can remaining go above budget or spent go below zero, which day they apply to); what 'Toggle' resumes. Also settle the canonical terms (e.g. 'log entry' vs 'adjustment', 'remaining' vs 'still yours').
