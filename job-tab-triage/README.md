# Job Tab Triage

A copy of the "I gave Claude my resume and 161 job posts" setup: Claude goes
through your open job tabs, scores each one against your resume, closes
everything under 75 and leaves the shortlist open, while a funnel dashboard
animates each verdict live.

- **Live dashboard:** https://claude.ai/artifact/XcQBmnxoTg1NRKTUD7MSSm
  (shows a sample run until the skill's first real run)
- **Skill:** `skill/job-tab-triage/SKILL.md`, also packaged as
  `job-tab-triage.zip`
- **Dashboard source:** `dashboard.html`

## Install the skill

1. In the Claude desktop app, open **Settings → Capabilities → Skills**.
2. Choose **Upload skill** and pick `job-tab-triage.zip`.
3. Make sure Claude in Chrome (or the built-in browser) is connected.

## Use it

Open the job postings you want sorted in Chrome, then tell Claude:

> Triage my job tabs

Keep the dashboard open to watch the funnel fill. When it finishes you get:

- The 75+ tabs still open, everything else closed
- A digest email in the same format as the daily Job Matches email
- Shortlist IDs (`20261014-T01`, ...) to hand to the **Apply to approved jobs**
  scheduled task, which writes the tailored resumes

## How it fits the existing routine

| Step | What runs | Output |
|---|---|---|
| Find postings | Job Matches (daily scheduled task) | Digest email, scored 1-10 |
| Clear out open tabs | Job Tab Triage (this skill) | Shortlist, scored 0-100 (75 = 7.5) |
| Tailor and apply | Apply to approved jobs (scheduled task) | Tailored resume and application |

## Dashboard data

The skill writes to the dashboard's database with the `ArtifactData` tool:

- `meta/current`: `{ run, threshold, label, source, sites }`. A new `run`
  value resets the funnel.
- `tabs/<id>`: one document per tab:
  `{ run, seq, title, company, site, url, score, verdict?, jobId, pay, type, reason }`.
