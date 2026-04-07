import cn.hutool.crypto.digest.BCrypt;

public class TestBCrypt {
    public static void main(String[] args) {
        String password = "123456";
        String hashed = BCrypt.hashpw(password);
        System.out.println("Hash for 123456: " + hashed);
        
        // Verify
        String storedHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS";
        boolean matches = BCrypt.checkpw(password, storedHash);
        System.out.println("Hash matches: " + matches);
    }
}
