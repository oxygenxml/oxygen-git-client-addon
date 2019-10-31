# Git Support Plugin

This add-on contributes a Git client directly in Oxygen XML Editor/Author/Developer, as a side-view.

## Installation

This add-on is compatible with Oxygen XML Editor/Author/Developer version 18.1 or higher. 

To install the add-on, follow these instructions:

1. Go to **Help > Install new add-ons...** to open an add-on selection dialog box.
2. Enter or paste https://raw.githubusercontent.com/oxygenxml/Oxygen-Git-Plugin/master/build/addon.xml in the **Show add-ons from** field.
3. Select the **Git Support** add-on and click **Next**.
4. Select the **I accept all terms of the end user license agreement** option and click **Finish**.
5. Restart the application.

**Result:** A **Git Staging** view will now be available in Oxygen. If it is not visible, go to **Window > Show View** and select **Get Staging**. This view acts as a basic Git client integrated directly in Oxygen, and it provides support for committing changes to a Git repository, comparing and merging changes, resolving conflicts, and other Git commands.

## Offline Installation

If you can't follow the previous procedure, for example because there is no internet access when using Oxygen XML Editor, you can manually install the add-on like this:
1. Go to the [Releases page](https://github.com/oxygenxml/oxygen-git-plugin/releases/latest) and download `git.support-{version}-plugin.jar`
2. Unzip it inside `{oXygenInstallDir}/plugins`. Make sure you don't create any any intermediate folders. After unzipping the archive, the file system should look like this: `{oXygenInstallDir}/plugins/git.support-x.y.z`, and inside this folder there should be a `plugin.xml`file.

## Cloning a repository

Click the **Clone new repository** button (it has a '+' sign as the icon) and provide the following:
- **Repository URL**: The URL of the remote repository to be cloned.
- **Checkout branch**: A specific branch of the repository to be cloned. The default branch will be cloned if another one is not specified.
- **Destination path**: The local path were the repository will be cloned.

After cloning a repository, it will automatically be set as the current working copy.

## Selecting a working copy

Click the **Browse** button to the right of the **Working copy** combo box to select a working copy from your file system. The selected folder must be a Git repository.

## Switching between local branches

To switch between local branches, use the **Change branch** action from the toolbar of the **Git Staging** view and select the desired branch from the presented combo box.

## Working with submodules

To open and work with a Git submodule, use the **Submodules** action from the toolbar and select the desired submodule from the presented combo box.

## Showing the current branch history

To show the history of the current branch, invoke the **Show current branch history** from the toolar (look for the clock icon).

## Unstaged resources area

In the *unstaged resources area* (the one at the top), you will see all the modifications that have occurred in your working copy (files that have been modified, new files, and deleted files), and which are not part of the next commit.
- Various actions are available in the contextual menu (**Open**, **Open in compare editor**, **Stage**, **Discard**, **Show in history**, **Show blame**, etc.).
- You can stage all the files by clicking the **Stage All** button or you can stage some of them by selecting and clicking the **Stage Selected** button.
- You can switch between the flat (table) view and the tree view by clicking on the **Switch to tree/flat view** button positioned to the right of the staging buttons.

## Staged resources area

In the *staged resources area*, you will see all the resources that are ready to be committed. The files from this area can be unstaged and sent back to the *unstaged resources area*. The *staged resources area* has actions similar to the ones from the *unstaged resources area*, with the mention that there are no **Show in history** and **Show blame** actions.

## Comparing changes and conflict resolution

At any time, if you want to see the differences between the last commit and your current modifications, you can double-click a file from either in the *unstaged resources area* or *staged resources area* and the [Oxygen's Diff](https://www.oxygenxml.com/doc/versions/19.0/ug-editor/topics/file-comparison-x-tools.html) window will appear and highlight the changes.

If the file has a conflict (has been modified both by you and another), [Oxygen's Three Way Diff](https://www.oxygenxml.com/doc/versions/19.0/ug-editor/topics/file-comparison-x-tools.html#file-comparison__threeway_comparisons) will show a comparison between the local change, the remote change, and the original base revision.

## Committing

After staging the files, on the bottom of the view you can provide a commit message and commit them to your local repository. For convenience, you can also select a previously provided message.

## Push/Pull

To push your local repository changes to the remote repository, use the **Push** button from the view's toolbar (up arrow). 

To bring the changes from the remote repository into your local repository, use one of the **Pull** actions from the same toolbar (down arrow). You can choose between **Pull (merge)** and **Pull (rebase)**. The invoked action is promoted as the current action of the toolbar button.

To push or pull, you need to access the remote, and for that you need to provide your credentials (if not using unprotected SSH keys). If no credentials are found, the add-on will ask for a username and a password. If you have a two-factor authentication for GitHub, you must go to your **Account Settings > Personal access tokens > Generate new token** and back in the **Git Staging** view in Oxygen, you have to use the generated token as the password when you are asked to enter your credentials.

## File conflicts solving flow

After editing a file, commiting it to the local repository and trying to push it to the remote repository, if a warning appears informing you about not being up-to-date with the repository, follow these steps:

1. Pull the data from the repository using the **Pull** button.
2. In the **Unstaged** area, select each conflicted file and open it the compare editor either by double-clicking or by using the contextual menu action. Choose what changes you want to keep/discard and save the document.
3. After you close the compare editor, the file will be staged automatically and moved to the **Staged** area.
4. When all the conflicts are resolved and no more files are left in the **Unstaged** area, the changes can be committed.
5. Enter a message and commit. You will now have a new change to **Push**.
6. Push the changes to the remote repository.

Copyright and License
---------------------
Copyright 2018 Syncro Soft SRL.

This project is licensed under [Apache License 2.0](https://github.com/oxygenxml/oxygen-git-plugin/blob/master/LICENSE)
