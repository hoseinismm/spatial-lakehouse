import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Viewpoint {
    String text;
    Map<String, Boolean> voteOfVotedUsers;
    User user;
    int likes;
    int dislikes;

    Viewpoint(String text, User user) {
        this.text = text;
        this.user = user;
        likes = 0;
        dislikes = 0;
        voteOfVotedUsers = new HashMap<String, Boolean>();
    }

    void vote(String username, Boolean isLike) {
        for (var entry : voteOfVotedUsers.entrySet()) {
            if (entry.getKey() == username) {
                if (entry.getValue()) {
                    likes--;
                } else {
                    dislikes--;
                }

                voteOfVotedUsers.remove(username);
                break;
            }
        }
        voteOfVotedUsers.put(username,isLike);
        if (isLike)
            likes++;
        else
            dislikes++;
    }

    Map<String, String> toJson(String action) {
        return {
                'text': text,
                'voteOfVotedUsers': voteOfVotedUsers,
                'user': user.toJson(),
                'likes': likes,
                'dislikes': dislikes,
                'action': action
    };
    }

    factory Viewpoint.fromJson(Map<String, dynamic> json) {
        return Viewpoint(
                json['text'],
                User.fromJson(json['user']),
                )
                ..voteOfVotedUsers = Map<String, bool>.from(json['voteOfVotedUsers'])
                ..likes = json['likes']
                ..dislikes = json['dislikes'];
    }
}

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    String id;
    String image;
    String largeimage;
    String description;
    int price;
    double score;
    int remained;
    String category;
    int salednumber;
    int discountPercent;
    String sellerName;
    int quantity;
    int priceAfterDiscount;
    int hasdiscount;
    ArrayList<Viewpoint> views;
    String introduction;

    Item(
    {required this.id,
            required this.image,
            required this.largeimage,
            required this.description,
            required this.category,
            required this.hasdiscount,
            required this.discountPercent,
            required this.price,
            required this.priceAfterDiscount,
            required this.salednumber,
            required this.score,
            required this.sellerName,
            required this.remained,
            required this.quantity,
            required this.introduction,
            required this.views});
    List<Viewpoint>? getViewpoints() {
        return views;
    }

    Map<String, dynamic> toJson2() {
        return {'action': "getItems"};
    }

    Map<String, dynamic> toJson(action) {
        return {
                'id': id,
                'image': image,
                'largeimage': largeimage,
                'description': description,
                'price': price,
                'score': score,
                'remained': remained,
                'category': category,
                'salednumber': salednumber,
                'discountPercent': discountPercent,
                'sellerName': sellerName,
                'quantity': quantity,
                'priceAfterDiscount': priceAfterDiscount,
                'hasdiscount': hasdiscount,
                'views': views.map((view) => view.toJson("")).toList(),
                'introduction': introduction,
                'action': action
    };
    } // ساخت یک شی از روی JSON

    factory Item.fromJson(Map<String, dynamic> json) {
        return Item(
                id: json['id'],
                image: json['image'],
                largeimage: json['largeimage'],
                description: json['description'],
                price: json['price'],
                score: json['score'],
                remained: json['remained'],
                category: json['category'],
                salednumber: json['salednumber'],
                discountPercent: json['discountPercent'],
                sellerName: json['sellerName'],
                quantity: json['quantity'],
                priceAfterDiscount: json['priceAfterDiscount'],
                hasdiscount: json['hasdiscount'],
                views: List<Viewpoint>.from(
                json['views'].map((view) => Viewpoint.fromJson(view)),
      ),
        introduction: json['introduction'],
    );
    }
}

class Items {
    late List<Item> items;

    void itemsSetPrice(Item item, int price) {
        item.price = price;
    }

    void itemsSetScore(Item item, double score) {
        item.score = score;
    }

    void itemsSetRemained(Item item, int remained) {
        item.remained = remained;
    }

    void itemsSetSellerName(Item item, String sellerName) {
        item.sellerName = sellerName;
    }

    void itemsSetSaledNumber(Item item, int salednumber) {
        item.salednumber = salednumber;
    }

    void itemsSetDiscountePercent(Item item, int discountPercent) {
        item.discountPercent = discountPercent;
    }

    void itemsSetQuantity(Item item, int quantity) {
        item.quantity = quantity;
    }

    List<Item> getItems() {
        return items;
    }

    Item? getItemByDescription(String description, List<Item> items) {
        for (var item in items) {
            if (item.description == description) {
                return item;
            }
        }
        return null;
    }

    List<Item> searchInItems(String word) {
        List<Item> results = [];
        for (var item in items) {
            if (item.description.contains(word)) {
                results.add(item);
            }
        }
        return results;
    }

    List<Item> shegeftangiz() {
        List<Item> results = [];
        for (var item in items) {
            if (item.discountPercent > 9) {
                results.add(item);
            }
        }
        return results;
    }

    List<Item> bartar() {
        List<Item> results = [];
        for (var item in items) {
            if (item.score > 3) {
                results.add(item);
            }
        }
        return results;
    }

    List<Item> categorize(String word) {
        List<Item> results = [];
        for (var item in items) {
            if (item.category == word) {
                results.add(item);
            }
        }
        return results;
    }
}
