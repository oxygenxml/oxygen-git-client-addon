---
name: Release procedure template
about: A template for the release procedure of each release.
title: ''
labels: ''
assignees: ''

---

- [ ] Make sure the automated test pass. Check Travis (https://travis-ci.org/github/oxygenxml/oxygen-git-plugin).
- [ ] Add the "What's New" list to **addon.xml**.
- [ ] Tag the commit that will be the last one included in the release and push the tag to the remote repository.
- [ ] Add the "What's New" list to the new release from GitHub. See https://github.com/oxygenxml/oxygen-git-plugin/releases.
- [ ] Send e-mail to the users who agreed to be notified when what they requested has been implemented.
- [ ] Update the forum threads where users requested things that have been implemented for the current release.
- [ ] Promote the release on social media, oxygen-users list and DITA list.
