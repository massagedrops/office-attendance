Firebase লিংক বানানোর নিয়ম (Windows)
1. https://nodejs.org থেকে Node.js ইনস্টল করুন (একবারই)।
2. Android Studio-তে প্রজেক্ট খুলে Build > Build APK(s) করুন।
3. এই ফোল্ডারের deploy.bat এ ডাবল ক্লিক করুন।
   - প্রথমবার Firebase Project ID চাইবে, আর ব্রাউজারে লগইন করতে হবে।
4. শেষে "Hosting URL" দেখাবে। ওই লিংকটাই শেয়ার করুন।
পরে নতুন ভার্সন দিতে: আবার Build APK করে deploy.bat চালান। লিংক একই থাকবে।

Android Studio ছাড়া: GitHub Actions দিয়ে বিল্ড করা APK ডাউনলোড করে
public\office-attendance.apk নামে এই ফোল্ডারের public এর ভিতরে রাখুন, তারপর deploy.bat চালান।
