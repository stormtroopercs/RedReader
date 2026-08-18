#!/bin/bash
cd /opt/data/RedReader
echo "########## JsonObject.kt (2) ##########"
grep '^e:.*jsonwrap/JsonObject' /tmp/compile.log | sed 's|.*JsonObject.kt:||'
echo "########## RedditAPI.kt (12) ##########"
grep '^e:.*reddit/RedditAPI.kt' /tmp/compile.log | sed 's|.*RedditAPI.kt:||'
echo "########## MainMenuFragment.kt (14) ##########"
grep '^e:.*fragments/MainMenuFragment.kt' /tmp/compile.log | sed 's|.*MainMenuFragment.kt:||'
echo "########## RedditURLParser.kt (10) ##########"
grep '^e:.*reddit/url/RedditURLParser.kt' /tmp/compile.log | sed 's|.*RedditURLParser.kt:||'
