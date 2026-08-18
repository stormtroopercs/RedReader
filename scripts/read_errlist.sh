#!/bin/bash
cd /opt/data/RedReader
echo "########## A. RedditAPI current errors ##########"
grep '^e:.*reddit/RedditAPI.kt' /tmp/compile.log | sed 's|.*RedditAPI.kt:||'
echo "########## B. NetWrapper errors ##########"
grep '^e:.*NetWrapper' /tmp/compile.log | sed 's|.*NetWrapper.kt:||'
echo "########## C. AnnouncementDownloader errors ##########"
grep '^e:.*AnnouncementDownloader' /tmp/compile.log | sed 's|.*AnnouncementDownloader.kt:||'
echo "########## D. ImageInfo errors ##########"
grep '^e:.*image/ImageInfo' /tmp/compile.log | sed 's|.*ImageInfo.kt:||'
echo "########## E. RedgifsAPIV2 errors ##########"
grep '^e:.*RedgifsAPIV2' /tmp/compile.log | sed 's|.*RedgifsAPIV2.kt:||'
echo "########## F. other IntDef/Retention files ##########"
grep '^e:.*RedditURLParser\|^e:.*MainMenuFragment.kt\|^e:.*BezelSwipeOverlay' /tmp/compile.log | sed 's|e: file://[^ ]*||'
