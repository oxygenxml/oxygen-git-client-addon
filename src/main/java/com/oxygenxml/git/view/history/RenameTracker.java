package com.oxygenxml.git.view.history;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RenameCallback;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Class to track all renames encountered during a {@link RevWalk}.
 *
 * @author alex_smarandache
 *
 * @see FollowFilter
 */
public class RenameTracker {

	/**
	 * The filter for RevCommit.
	 * <br><br>
	 * Should be applied with {@link org.eclipse.jgit.revwalk.RevWalk#setRevFilter(RevFilter newFilter)}
	 * on a RevWalk or PlotWalk.
	 */
	private final RevFilter filter = new RevFilter() {

		@Override
		public boolean include(final RevWalk walker, final RevCommit commit)
				throws IOException {
			if (currentPath != null) {
				filePathOnCommitMap.put(commit, currentPath);
			} else if (currentDiff != null) {
				filePathOnCommitMap.put(commit, currentDiff.getNewPath());
				currentPath = currentDiff.getOldPath();
				currentDiff = null;
			}
			
			return true;
		}

		@SuppressWarnings("java:S1182")
		@Override
		public RevFilter clone() {
			return this;
		}

	};

	
	/**
	 * The RenameCallback.
	 * <br><br>
	 * Should be set on a {@link org.eclipse.jgit.revwalk.FollowFilter} which is applied with
	 * {@link org.eclipse.jgit.revwalk.RevWalk.setTreeFilter(TreeFilter newFilter)}.
	 */
	private final RenameCallback callback = new RenameCallback() {

		@Override
		public void renamed(final DiffEntry entry) {
			currentDiff = entry;
			currentPath = null;
		}
	};

	
	/**
	 * The current diff entry.
	 */
	private DiffEntry currentDiff;

	/**
	 * The current followed path.
	 */
	private String currentPath;

	/**
	 * A map that contains the RevCommit and the value as the key is the path followed at that moment.
	 */
	private final Map<RevCommit, String> filePathOnCommitMap = new LinkedHashMap<>();
	
	/**
	 * The initial path.
	 */
	private String initialPath = null;

	
	/**
	 * @return The filter.
	 * 
	 * <br><br>
	 * Should be applied with {@link org.eclipse.jgit.revwalk.RevWalk#setRevFilter(RevFilter newFilter)}
	 * on a RevWalk or PlotWalk.
	 */
	public RevFilter getFilter() {
		return filter;
	}

	
	/**
	 * @return The rename call back. 
	 * <br><br>
	 * Should be set on a {@link org.eclipse.jgit.revwalk.FollowFilter} which is applied with
	 * {@link org.eclipse.jgit.revwalk.RevWalk.setTreeFilter(TreeFilter newFilter)}.
	 */
	public RenameCallback getCallback() {
		return callback;
	}
	

	/**
	 * Get file path value on a target commit version.
	 *
	 * @param target        The target commit.
	 *
	 * @return path         The path in commit.
	 */
	public String getPath(final ObjectId target) {
		return filePathOnCommitMap.get(target);
	}

	
	/**
	 * @return The initial path of searched file.
	 */
	public String getInitialPath() {
		return initialPath;
	}
	
	
	/**
	 * Reset the tracker for a new path.
	 *
	 * @param path          The new path.
	 * 
	 */
	public void reset(final String path) {
		filePathOnCommitMap.clear();
		currentPath = path;
		initialPath = path;
	}
	
}
