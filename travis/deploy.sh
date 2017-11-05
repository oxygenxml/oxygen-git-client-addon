git config user.name "Alex Jitianu";
git config user.email "alex_jitianu@oxygenxml.com";
git fetch;
git checkout master;
git reset;
cp -f target/addon.xml build;
git add build/addon.xml;
git commit -m "New release - ${TRAVIS_TAG}";
git push origin HEAD:master; 
