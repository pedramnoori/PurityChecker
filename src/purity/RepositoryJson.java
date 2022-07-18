package purity;

import java.util.List;

public class RepositoryJson {

    private int id;
    private String repository;
    private String sha1;
    private String url;
    private String author;
    private String time;
    private List<RefactoringJson> refactorings;
    public long refDiffExecutionTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<RefactoringJson> getRefactorings() {
        return refactorings;
    }

    public void setRefactorings(List<RefactoringJson> refactorings) {
        this.refactorings = refactorings;
    }

    public long getRefDiffExecutionTime() {
        return refDiffExecutionTime;
    }

    public void setRefDiffExecutionTime(long refDiffExecutionTime) {
        this.refDiffExecutionTime = refDiffExecutionTime;
    }
}
