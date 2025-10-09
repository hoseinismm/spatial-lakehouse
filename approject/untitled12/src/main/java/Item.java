import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;










public class Item  {








    int id;
    String image;
    String largeimage;
    String description;
    int price;
    double score;



    String category;
    int salednumber;
    int discountPercent;
    String sellerName;
    int quantity;
    int priceAfterDiscount;
    int hasdiscount;

    String introduction;

    public Item(int id, String image, String largeImage, String description, int price, double score,
                int salednumber,int hasDiscount, int discountPercent,int quantity, String sellerName,
                String category, String introduction) {
        this.id = id;
        this.image = image;
        this.largeimage = largeImage;
        this.description = description;
        this.price = price;
        this.score = score;
        this.category = category;
        this.salednumber = salednumber;
        this.discountPercent = discountPercent;
        this.sellerName = sellerName;
        this.quantity = quantity;
        priceAfterDiscount = (int) (price * (100-discountPercent) / 100);
        this.hasdiscount = hasdiscount;

        this.introduction = introduction;
    }


}








