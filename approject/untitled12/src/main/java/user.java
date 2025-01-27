import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum OrderStatus { Pending, confirmed, Delivered }

class Receipt {
    static int count = 0;
    int id;
    List<Item> kalahayekharidarishode;
    LocalDateTime timeofshopping;
    int totalprice;
    OrderStatus orderStatus;

    Receipt(this.kalahayekharidarishode, this.totalprice)
      : timeofshopping = DateTime.now(),
    id = ++count,
    orderStatus = OrderStatus.Pending;
}

class User {
    String userid;
    String profileImage;
    String username;
    String email;
    String password;
    ArrayList<Item> favorites;
    ArrayList<Item> shoppingcart;
    Map<Item,Integer> numberkala;
    ArrayList<Receipt> receipt;
    boolean plusSubscription;
    LocalDateTime endOfSubscription;

    User(int userid,
         String username,
         String password,
         String email,
         LocalDateTime endOfSubscription)
    {
        this.username = username;
        this.password = password;
        this.email = email;
        Map<Item,Integer> numberkala=new HashMap<Item,Integer>();
        ArrayList<Receipt> receipt= new  ArrayList<Receipt> ();
        ArrayList<Item> favorites=new ArrayList<Item>();
        ArrayList<Item> shoppingcart= new  ArrayList<Item> ();
        plusSubscription=false;
        profileImage="/assets/default.jpg";
    }







    void registerUser(int days) {
        if ((endOfSubscription == null) ||
                (endOfSubscription.isBefore(LocalDateTime.now()))) {
            endOfSubscription =  LocalDateTime.now().plusDays(days);
        } else {
            endOfSubscription = endOfSubscription.plusDays(days);
        }
    }

    void addnumberkala(Item item) {
        if (numberkala.containsKey(item)) {
            numberkala.put(item,numberkala.get(item) + 1);
        } else {
            numberkala.put(item, 1);
        }
    }

    void decreasenumberkala(Item item) {
        if (numberkala.containsKey(item)) {
            if (numberkala.get(item) > 1) {
                numberkala.put(item, numberkala.get(item) - 1);
            } else if (numberkala.get(item) == 1) {
                numberkala.remove(item);
            }
        }
    }

    int totalshoppingcart() {
        int total = 0;
        for (var i : numberkala.entrySet()) {
            total += i.getKey().priceAfterDiscount * i.getValue();
        }
        return total;
    }

    static boolean isValidEmail(String email) {
        String emailPattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$';
        RegExp regex = RegExp(emailPattern);
        return regex.hasMatch(email);
    }

    static bool isValidPassword(String password, String username) {
        String pattern = r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d]{8,}$';
        RegExp regex = RegExp(pattern);
        if (!regex.hasMatch(password)) {
            return false;
        }
        if (password.contains(username)) {
            return false;
        }
        return true;
    }

    bool setPassword(String password, String username) {
        if (isValidPassword(password, username)) {
            this.password = password;
            return true;
        }
        return false;
    }

    void setEmail(String email) {
        if (isValidEmail(email)) {
            this.email = email;
        }
    }
}
/*
class Users {
  List<User> users = [];


  Users() {
    User a = User(
        username: 'hoseini',
       password: '13811381aA',
        email: 'hoseini_smm@yahoo.com');
    User b = User(username: 'ab', password: '11111111aA', email: 'a@a.aa');
    User c = User(username: 'ab2', password: '11111111bB', email: 'b@b.bb');
    users.add(a);
    users.add(b);
    users.add(c);
  }

  User? checkinput(String username, String password) {
    for (User a in users) {
      if ((a.username == username) && (a.password == password)) {
        return a;
      }
    }
    return null;
  }

  String? changeusername(String username) {
    for (User a in users) {
      if (a.username == username) {
        return "Username already taken";
      }
    }
    return null;
  }

  String? changeemail(String email) {
    if (!User.isValidEmail(email))
      return "Username already taken";
    else
      return null;
  }

  String? changepassword(String password, String username) {
    if (!User.isValidPassword(password, username))
      return "Password must be at least 8 characters long and contain both capital letters and digits. Also, it should not contain the username.";
    else
      return null;
  }

  String adduser(String username, String password, String email) {
    for (User a in users) {
      if (a.username == username) {
        return "Username already taken";
      }
    }
    if (!User.isValidEmail(email)) {
      return "Email format is not correct!";
    }
    if (!User.isValidPassword(password, username)) {
      return "Password must be at least 8 characters long and contain both capital letters and digits. Also, it should not contain the username.";
    }
    User newUser = User(username: username, password: password, email: email);
    this.users.add(newUser);
    return 'User successfully created.';
  }

  User? login(String username, String password) {
    for (User a in users) {
      if (((username == a.username) || (username == a.email)) &&
          (password == a.password)) {
        return a;
      }
    }
    return null;
  }
}

*/

