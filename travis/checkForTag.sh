if [[ ! -z ${TRAVIS_TAG} ]] 
then mvn versions:set -DnewVersion="${TRAVIS_TAG}";
echo "The tag is -------------------------------- ${TRAVIS_TAG}";
fi
