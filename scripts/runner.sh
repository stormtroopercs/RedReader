#!/bin/bash
cd /opt/data/RedReader.wiki
git add Home.md Architecture.md
git -c user.name=stormtroopercs -c user.email=1967616+stormtroopercs@users.noreply.github.com commit -m "Roadmap + Architecture: 1,008 build-verified, de-null unit in flight

Refresh both pages to the current state: build error count 1,008
(2026-08-17), src/main 498 .kt / 0 .java, 16 Java test sources to
convert. Phase 4d marked closed; the faithful de-null root-cause unit
(over-nulled common/collections + io/ base types) documented as in
progress with the two converter bugs it surfaced (annotation-import
shadowing, RawObjectDB self-syntax). Update top-20 error table and
the migration one-liner accordingly."
git log --oneline -3
echo "===== push ====="
git push origin master
