package org.jenkinsci.plugins.scmanywhere;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.scm.ChangeLogSet.AffectedFile;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * This contain all the changes in the workspace. These objects are used to build the change log page.
 */
@ExportedBean(defaultVisibility = 999)
public class SCMChangeLogEntry extends ChangeLogSet.Entry {
    private final String changeSetId;
    private final String user;
    private final String dateTime;
    private final String comments;
    private String action;
    private List<ModifiedFile> modifiedFiles;

    /**
     * Creates a new SCM any here ChangeLogEntry object containing all the details about.
     *
     * @param changeSetId The change ID of the log
     * @param user The user of the action
     * @param dateTime The file action date time stamp
     * @param action The type of action
     * @param modifiedFiles A list of ModifiedFiles impacted by the commit
     */
    public SCMChangeLogEntry(final String changeSetId, final String user, final String dateTime, final String comments,
            final List<ModifiedFile> modifiedFiles) {
        this.changeSetId = changeSetId;
        this.user = user;
        this.dateTime = dateTime;
        this.comments = comments;
        this.modifiedFiles = modifiedFiles;
    }

    /**
     * Converts this ChangeLogEntry to a string for debugging.
     *
     * @return A String of change log entry information.
     */
    @Override
    public String toString() {
        return " changeSet ID : " + changeSetId + "\n" +
                "user: " + user + "\n" +
                "dateTime: " + dateTime + "\n" +
                "action: " + action + "\n" +
                "comments: " + comments + "\n" +
                "modifiedFiles: " + modifiedFiles;
    }

    /**
     * Returns the author name
     */
    @Exported
    public String getUser() {
        return user;
    }

    /**
     * Returns the time stamp.
     */
    @Exported
    public String getDateTime() {
        return dateTime;
    }

    /**
     * Returns type of action on the file
     */
    @Exported
    public String getAction() {
        return action;
    }

    /**
     * Returns comments on the file
     */
    @Exported
    public String getComments() {
        return comments;
    }

    /**
     * Returns a list of files modified by this change.
     */
    @Exported
    public List<ModifiedFile> getModifiedFiles() {
        return modifiedFiles;
    }

    /**
     * Returns a set of paths in the workspace that was affected by this change.
     */
    @Override
    public List<ModifiedFile> getAffectedFiles() {
        return modifiedFiles;
    }

    @Override
    public String getMsg() {
        return comments;
    }

    @Override
    public User getAuthor() {
        if (user == null) {
            return User.getUnknown();
        }
        return User.get(user);
    }

    @Override
    public void setParent(@SuppressWarnings("rawtypes") final ChangeLogSet parent) {
        super.setParent(parent);
    }

    @Override
    public Collection<String> getAffectedPaths() {
        if (modifiedFiles == null) {
            return null;
        }
        return new AbstractList<String>() {
            @Override
            public String get(final int index) {
                return modifiedFiles.get(index).getPath();
            }

            @Override
            public int size() {
                return modifiedFiles.size();
            }
        };
    }

    @Exported
    public String getChangeSetId() {
        return changeSetId;
    }

    public static class ModifiedFile implements AffectedFile {

        /**
         * The SCM any where standard of showing the each file action
         */
        public static final EditType EDITED = new EditType("EDITED", "The file was edited");
        public static final EditType CREATED = new EditType("CREATED", "The file was created");
        public static final EditType ADDED = new EditType("ADDED", "The file was added");
        public static final EditType DELETED = new EditType("DELETED", "The file was deleted");
        public static final EditType DELETED_PURGED = new EditType("DELETED , PURGED", "The file was deleted and purged");
        public static final EditType PARENT_DELETED = new EditType("PARENT DELETED ", "The parent file was deleted");
        public static final EditType PARENT_DELETED_PURGED = new EditType("PARENT DELETED , PURGED", "The parent file was deleted and purged");
        private final String path;
        private final char action;

        /**
         * Create a new ModifiedFile object with the given path and action.
         *
         * @param path the path of the file
         * @param action the action performed on the file, as reported by Git (A for added, D for deleted, C for Created, etc)
         */
        public ModifiedFile(final String path, final char action) {
            this.path = path;
            this.action = action;
        }

        /**
         * Returns the path of the file.
         */
        @Exported
        public String getPath() {
            return path;
        }

        /**
         * Returns the action performed on the file.
         */
        @Exported
        public char getAction() {
            return action;
        }

        /**
         * Returns the EditType performed on the file (based on the action).
         */
        @Exported
        public EditType getEditType() {
            if (action == 'A') {
                return ADDED;
            } else if (action == 'D') {
                return DELETED;
            } else if (action == 'E') {
                return EDITED;
            } else if (action == 'C') {
                return CREATED;
            } else if (action == 'U') {
                return DELETED_PURGED;
            } else if (action == 'P') {
                return PARENT_DELETED;
            } else if (action == 'G') {
                return PARENT_DELETED_PURGED;
            } else {
                return new EditType("unknown: " + action, "An unknown file action");
            }
        }
    }
}
