# Git support plugin

This add-on is compatible with Oxygen XML Editor (or XML Author) version 17.0 or higher. 

You can install the add-on by using Oxygen's add-ons support. In Oxygen, go to Help->Install new add-ons... and paste:

https://raw.githubusercontent.com/oxygenxml/Oxygen-Git-Plugin/master/build/addon.xml

and continue the installation.

After installing the plugin if the git window is not visible you should go to the "Window" menu -> show view -> Git Staging.

Selecting the working copy
=========================

Click the *Browse* button to select a working copy from your file system. The selected folder must be a Git Repository. The plugin assumes that you already have a local clone of the remote repository.

Unstaged resources area
========================
In the unstaged resources area (the one on top) you will see all the modifications that have occurred since your last commit (files that have been modified, new files and deleted files). Various actions are available in the contextual menu (*Open*, *Open in compare editor*).
You can stage all the files by clicking the *Stage All* button or you can stage some of them by selecting and clicking the *Stage Selected* button. 
You can switch from the flat(table) view to a tree view by clicking on the button positioned above area.

Staged resources area
=====================
In the staging area you will see all the resources that are ready to be committed. Any files from this area can be unstaged and sent back to the unstaged resources area. The staging area has similar actions with the unstaging area.

Comparing changes and conflict resolution
==========================================
At any time, if you want to see the differences between the last commit and your current modifications you can double click the file that appears either in the staging or unstaging area and the [Oxygen's Diff](https://www.oxygenxml.com/doc/versions/19.0/ug-editor/topics/file-comparison-x-tools.html) window will appear and highlight the changes.
If the file is in conflict (has been modified both by you and another), [Oxygen's Three Way Diff](https://www.oxygenxml.com/doc/versions/19.0/ug-editor/topics/file-comparison-x-tools.html#file-comparison__threeway_comparisons) will show a comparison between the local change, the remote change, and the original base revision.

Committing
==========
After staging the files, on the bottom of the view you can input the commit message and commit them into your local repository. For convenience, you can also select one of the previously entered messages.

Push/Pull
=========
To push your local repository changes to the remote one you must click on the *Push* button from the view's toolbar. To bring the changes from the remote repository into your local one you must click on the *Pull* button from the same toolbar.
To push or pull you need to acces the remote, and for that you need to provide some credentials. If no credentials are found , the addon will ask for an account and password. If you have a two-factor authentication for: 
-GitHub: You must go to your *Account Settings* -> *Personal access tokens* -> *Generate new token*. After that you have to use the generated token as the password back in Oxygen when you are asked to enter your credentials
