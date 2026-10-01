# Data model

Type: grilling
Status: open
Blocked by: 02, 03

## Question

What is stored, and in what shape? Settle: append-only event log vs current-state tables (or both, with a projection); the event types; how today's remaining and spent are derived; device identity and ordering so a watch can join later; what the exported SQLite file contains and whether its schema is a documented, stable contract; how priority deletion and edits appear in history.
