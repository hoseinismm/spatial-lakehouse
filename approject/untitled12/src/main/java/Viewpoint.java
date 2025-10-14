public class Viewpoint {
    String review_text;
    int review_id;
    int likes;
    int dislikes;
    String username;

    Viewpoint(String text, int id, int likes, int dislikes,String username) {
        this.review_text = text;
        this.review_id = id;
        this.likes = likes;
        this.dislikes = dislikes;
        this.username = username;
    }
}