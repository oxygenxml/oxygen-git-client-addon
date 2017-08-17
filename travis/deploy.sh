git config user.name "BeniaminSavu";
git config user.email "benisavu@gmail.com";
git fetch;
git checkout master;
git reset;
git add target/addon.xml;
git status;
git commit -m "New release - ${TRAVIS_TAG}";
git push origin HEAD:master;