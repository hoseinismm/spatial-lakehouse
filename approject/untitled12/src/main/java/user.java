import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.in;

enum OrderStatus { Pending, confirmed, Delivered }

class Receipt {
    static int count = 0;
    int id;
    List<Integer> kalahayekharidarishode;
    LocalDateTime timeofshopping;
    int totalprice;
    OrderStatus orderStatus;

    Receipt(List<Integer> kalahayekharidarishode, Integer totalprice) {
        this.id = count++;
        this.totalprice = totalprice;
        this.orderStatus = OrderStatus.Pending;
        this.timeofshopping = LocalDateTime.now();
        this.kalahayekharidarishode = kalahayekharidarishode;
    }
}


class User {
    int userid;

    String image;
    String username;
    String email;
    String password;
    ArrayList<Integer> favorites;
    Map<Integer, Integer> shoppingcart;

    int is_subscriber;
    String Subscriptionend;

    User(String image,int userid, String username, String password, String email, int is_subscriber, ArrayList<Integer> favorites, Map<Integer, Integer> shoppingcart) {
        this.image = image;
        this.userid = userid;
        this.username = username;
        this.password = password;
        this.email = email;
        this.is_subscriber = is_subscriber;
        this.favorites = favorites;
        this.shoppingcart = shoppingcart;
    }

    void setSubscriptionend(String Subscriptionend) {
        this.Subscriptionend = Subscriptionend;
    }
}
