# Git support plugin


After installing the plugin if the git window is not visible you should go to the "Window" menu -> show view -> Git Staging.

Usage:
- Select a working copy from your file system. The folder
selected must be a git Repository otherwise the plugin will prompt
you with an error message. The plugin assumes you already have a
checkout repository.
- After you have selected the repository the
plugin will show you all the modification that have occurred since
your last commit in the unstaging area (files that have been
modified, new files and deleted files) with their location
relative to the working copy and a button to individually stage
them.
- You can then select to stage all those files by
clicking on the "Stage All" button or some of them by selecting
the files you want and clicking on the "Stage Selected" button or
you can individually send a file to the staging area by clicking
the "Stage" button that appears on the right for each file.
- If you don't like the flat(table) view you can opt for a tree view
by clicking on the button positioned above the table to the
right. It has the same functionality
- Once the files are in the taging area they are ready to be commited. But if you change
your mind regarding one of the files that you previously staged
you can of course unstage that file or all of them and commit
only the files you want. Basically the staging area works the
same as the unstaging area. Same buttons with the same
functionality.
- If you want to see the difference between the
last commit and your current modifications you can double click
the file that appears in the staging or unstaging area and a diff
window will appear highlighting the changes.
- Once you are
satisfied with your file choice you can add a commit message in
the text area and click on the commit button. The files will be
commited locally on your system. To send them to the remote
repository you must click on the push button(is the first button
on the top left corner, represented by an "up arrow"). Ofcourse if
you have changes that you want to bring from the remote
repository you have the option to "pull" them using the second
button located also in the top left corner represented by a "down
arrow".
