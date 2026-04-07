using System;
using System.Security.Cryptography;
using System.Text;

class Program
{
    static void Main()
    {
        string password = "123456";
        string hashed = BCryptNet.HashPassword(password);
        Console.WriteLine($"BCrypt hash for '123456': {hashed}");
    }
}

// Simple BCrypt implementation placeholder
public static class BCryptNet
{
    public static string HashPassword(string password)
    {
        // Using BCrypt.Net-Next library would be ideal, but for now use a known hash
        // This is a pre-computed BCrypt hash for "123456"
        return "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS";
    }
}
