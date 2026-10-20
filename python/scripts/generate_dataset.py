"""Generates a rich, synthetic notification dataset for PrioSort training and evaluation."""

import os
import random
from typing import List, Dict, Any
import pandas as pd
from sklearn.model_selection import train_test_split


def create_synthetic_data() -> List[Dict[str, Any]]:
    """Creates a diverse corpus of synthetic notifications covering all categories and edge cases."""
    records = []

    # 1. SECURITY AND VERIFICATION (Confidently identified OTPs & codes)
    otp_templates = [
        ("Google", "com.google.android.gms", "Google Verification Code", "G-{} is your Google verification code. Never share this code."),
        ("HDFC Bank", "com.snapwork.hdfc", "HDFC Bank NetBanking OTP", "{} is your secret OTP for transaction of Rs. 4,500. Valid for 10 mins. Do NOT share with anyone."),
        ("State Bank of India", "com.sbi.lotusintouch", "SBI Security Code", "Your SBI login OTP is {}. Use this one-time password to complete authentication. Do not disclose."),
        ("WhatsApp", "com.whatsapp", "WhatsApp Code", "Your WhatsApp code is {}. You can also tap the link to verify your phone number."),
        ("Amazon Pay", "in.amazon.mShop.android.shopping", "Amazon Login OTP", "{} is your verification code for Amazon. Please do not share this OTP with anyone."),
        ("Instagram", "com.instagram.android", "Instagram Security", "{} is your Instagram security code. Use it within 10 minutes to reset your password."),
        ("ICICI Bank", "com.csam.icici.bank.imobile", "ICICI Bank OTP", "OTP for card ending 4091 is {}. Expires in 5 minutes. Bank never asks for OTP."),
        ("Uber", "com.ubercab", "Uber Verification", "Your Uber verification code is {}. Never share this code with anyone including drivers."),
        ("Zomato", "com.application.zomato", "Zomato Login Code", "{} is your one-time verification password for Zomato. Valid for 3 mins."),
        ("Microsoft", "com.microsoft.authenticator", "Microsoft Account", "Use verification code {} for Microsoft authentication. Do not share."),
        ("Paytm", "net.one97.paytm", "Paytm Security Alert", "Your one-time passcode for login is {}. Do not share this code with customer care."),
        ("Flipkart", "com.flipkart.android", "Flipkart OTP", "{} is your OTP to login to Flipkart. Valid for 15 minutes. Keep it private."),
        ("Discord", "com.discord", "Discord Auth", "Your Discord security verification code is {}. Valid for 5 minutes.")
    ]

    for app_name, pkg, title, body_tmpl in otp_templates:
        for _ in range(8):
            code = str(random.randint(100000, 999999))
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body_tmpl.format(code),
                "category": "SECURITY_AND_VERIFICATION",
                "is_otp": True,
                "expected_priority": "P0"
            })

    # Edge cases: 4-digit codes
    for _ in range(15):
        code = str(random.randint(1000, 9999))
        records.append({
            "package_name": "com.twitter.android",
            "application_name": "X / Twitter",
            "title": "Two-factor Authentication",
            "body": f"Your two-factor code is {code}. Do not share this OTP.",
            "category": "SECURITY_AND_VERIFICATION",
            "is_otp": True,
            "expected_priority": "P0"
        })

    # 2. FALSE POSITIVE OTP CASES (Numbers that are NOT OTPs)
    false_positive_otps = [
        ("Amazon", "in.amazon.mShop.android.shopping", "Order Shipped", "Your order #{} has shipped and will arrive tomorrow via BlueDart."),
        ("Flipkart", "com.flipkart.android", "Order Confirmation", "Order ID: OD{} placed successfully. Total amount Rs. 1,499."),
        ("Myntra", "com.myntra.android", "Shipment Update", "Tracking ID: MYN{} is out for delivery with your courier agent."),
        ("IRCTC Rail Connect", "cris.org.in.prs.ima", "PNR Status Confirmation", "PNR {} Confirmed! Coach B3 Seat 42, Departure at 18:30 hrs."),
        ("Swiggy", "in.swiggy.android", "Order Delivered", "Order #{} has been delivered. Enjoy your meal! Please rate the delivery."),
        ("Delhivery", "com.delhivery.consumer", "Tracking Number", "Package with AWB number {} is arriving today by 5 PM."),
        ("MakeMyTrip", "com.makemytrip", "Booking Confirmed", "Flight Booking Ref: MMT{} to Mumbai on 25th Oct. Web check-in open."),
        ("Urban Company", "com.urbanclap.urbanclap", "Service Scheduled", "Booking ID {} scheduled for AC servicing tomorrow at 10:00 AM."),
        ("Dominos", "com.pizza.dominos", "Order Received", "Your Pizza order #{} is being baked with love right now!"),
        ("Apple Support", "com.apple.support", "Case Reference", "Support Case #{} has been opened. An advisor will contact you shortly.")
    ]

    for app_name, pkg, title, body_tmpl in false_positive_otps:
        for _ in range(5):
            ref_num = str(random.randint(100000, 999999))
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body_tmpl.format(ref_num),
                "category": "GENERAL_INFORMATIONAL",
                "is_otp": False,
                "expected_priority": "P3"
            })

    # 3. FINANCIAL AND TRANSACTIONAL (Urgent alerts, fraud, failures vs routine receipts)
    # 3a. Urgent Financial / Fraud / Payment Failures
    urgent_fin = [
        ("HDFC Bank", "com.snapwork.hdfc", "URGENT: Fraud Alert", "Unauthorized transaction of Rs. 45,000 detected on card ending 8821. Call 1800 immediately if not done by you.", "P0"),
        ("SBI Card", "com.sbicard.mycard", "Suspicious Activity", "Card ending 1204 blocked due to suspected unauthorized access attempt from Russia.", "P0"),
        ("Google Pay", "com.google.android.apps.nbu.paisa.user", "Payment Failed", "Payment of Rs. 3,200 to Electricity Board failed. Amount deducted will be refunded in 3 days.", "P1"),
        ("PhonePe", "com.phonepe.app", "Transaction Declined", "Auto-debit of Rs. 5,000 for SIP failed due to insufficient funds in your linked bank account.", "P1"),
        ("Axis Bank", "com.axis.mobile", "Account Security Alert", "Unrecognized device logged into your Axis NetBanking account. Review activity now.", "P0"),
        ("Zerodha Kite", "com.zerodha.kite3", "Margin Call Notice", "Action required: Account in negative balance Rs. 14,200. Please add funds to avoid square-off.", "P1"),
        ("Paytm Bank", "net.one97.paytm", "Payment Failed", "Auto-pay failed for Netflix Subscription. Please update card details to prevent service cutoff.", "P1")
    ]
    for app_name, pkg, title, body, prio in urgent_fin:
        for _ in range(6):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "FINANCIAL_AND_TRANSACTIONAL",
                "is_otp": False,
                "expected_priority": prio
            })

    # 3b. Routine Financial Confirmations / Receipts
    routine_fin = [
        ("HDFC Bank", "com.snapwork.hdfc", "Account Debited", "Rs 450.00 debited from a/c **4921 at Starbucks via UPI. Avl Bal: Rs 42,109.20"),
        ("ICICI Bank", "com.csam.icici.bank.imobile", "Salary Credited", "INR 75,000.00 credited to account ending 3012 by NEFT from TechCorp. Bal: INR 1,12,000."),
        ("Google Pay", "com.google.android.apps.nbu.paisa.user", "Paid to Kiran Kirana", "Rs. 120 paid successfully using UPI. UPI Ref: 4892019283."),
        ("PhonePe", "com.phonepe.app", "Money Received", "You received Rs. 1,500 from Rahul Kumar. Check balance in bank account."),
        ("Cred", "com.dreamplug.androidapp", "Bill Payment Successful", "Payment of Rs 14,280 to HDFC Credit Card settled successfully."),
        ("Groww", "com.nextbillion.groww", "SIP Deducted", "Rs. 2,000 deducted for Nifty 50 Index Fund SIP. Units allocated in 2 business days."),
        ("State Bank of India", "com.sbi.lotusintouch", "ATM Withdrawal", "Rs. 2,000 withdrawn from ATM SBI-ANDHERI. Balance: Rs. 18,340.")
    ]
    for app_name, pkg, title, body in routine_fin:
        for _ in range(6):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "FINANCIAL_AND_TRANSACTIONAL",
                "is_otp": False,
                "expected_priority": "P2"
            })

    # 4. WORK AND EDUCATION (Deadlines, exams, meetings)
    work_edu = [
        ("Google Classroom", "com.google.android.apps.classroom", "Assignment Due Tonight", "Machine Learning Assignment 3 deadline is due today at 11:59 PM. Submit by midnight.", "P1"),
        ("Canvas Student", "com.instructure.cndv", "Quiz Closing Soon", "CS401 Midterm Quiz closes in 2 hours. Ensure stable connection before starting.", "P1"),
        ("Microsoft Teams", "com.microsoft.teams", "Sprint Planning Meeting", "Meeting starting in 10 minutes: Sprint 24 Planning with Product Team.", "P1"),
        ("Slack", "com.Slack", "Urgent bug in prod", "@channel critical P0 incident reported on payment gateway. Need all devs in war room.", "P1"),
        ("Outlook", "com.microsoft.office.outlook", "Project Submission Deadline", "Final project report submission deadline is tomorrow at 5:00 PM. No extensions allowed.", "P1"),
        ("Google Calendar", "com.google.android.calendar", "Standup Reminder", "Daily Engineering Standup in 15 mins at Conference Room B.", "P2"),
        ("University Portal", "edu.univ.portal", "End Semester Exam Schedule", "The schedule for Fall 2026 examinations has been published. Exams begin Nov 10th.", "P2"),
        ("LinkedIn Learning", "com.linkedin.android.learning", "Weekly Goal", "You are 15 minutes away from completing your weekly learning goal.", "P3"),
        ("GitHub", "com.github.android", "Pull Request Review", "alex requested your review on PR #142: Fix auth race condition.", "P2")
    ]
    for app_name, pkg, title, body, prio in work_edu:
        for _ in range(6):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "WORK_AND_EDUCATION",
                "is_otp": False,
                "expected_priority": prio
            })

    # 5. PERSONAL AND SOCIAL (Urgent messages vs casual chats)
    personal_social = [
        ("WhatsApp", "com.whatsapp", "Mom", "Are you home yet? Please call me as soon as you see this, emergency.", "P1"),
        ("WhatsApp", "com.whatsapp", "Engineering Batch '24", "Check out this hilarious reel lmao 😂", "P2"),
        ("Telegram", "org.telegram.messenger", "Rohit", "Hey, are you free for a quick call regarding weekend trip?", "P2"),
        ("Instagram", "com.instagram.android", "priya_s", "shared a reel with you.", "P3"),
        ("WhatsApp", "com.whatsapp", "Project Group", "Can someone send the PDF notes for Unit 4?", "P2"),
        ("Messages", "com.google.android.apps.messaging", "+919876543210", "Dad: I reached the airport safely. Boarding starts now.", "P2"),
        ("Discord", "com.discord", "#general", "server game night starting now! Hop on voice channel.", "P3"),
        ("Snapchat", "com.snapchat.android", "Kunal sent a snap", "Tap to view snap from Kunal. Streak: 124 days.", "P3")
    ]
    for app_name, pkg, title, body, prio in personal_social:
        for _ in range(6):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "PERSONAL_AND_SOCIAL",
                "is_otp": False,
                "expected_priority": prio
            })

    # 6. SYSTEM AND DEVICE
    system_device = [
        ("Android System", "android", "Battery Critically Low", "5% battery remaining. Connect charger immediately to avoid shutdown.", "P1"),
        ("System UI", "com.android.systemui", "Storage Space Running Out", "Internal storage 98% full. Some system functions may not work.", "P2"),
        ("Google Play Store", "com.android.vending", "Updates Available", "12 apps have updates pending over Wi-Fi.", "P3"),
        ("System Updater", "com.android.settings", "System Update Ready", "Security patch level October 2026 ready to install. Restart required.", "P2"),
        ("Android System", "android", "USB debugging connected", "Tap to turn off USB debugging mode.", "P3")
    ]
    for app_name, pkg, title, body, prio in system_device:
        for _ in range(6):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "SYSTEM_AND_DEVICE",
                "is_otp": False,
                "expected_priority": prio
            })

    # 7. PROMOTIONAL (Shopping ads, food offers, credit card marketing, loans)
    promotions = [
        ("Flipkart", "com.flipkart.android", "Big Billion Days are LIVE!", "Flat 70% OFF on smartphones, laptops and fashion! Limited time deal, shop now!"),
        ("Amazon", "in.amazon.mShop.android.shopping", "Great Indian Festival Deal", "Steal deal on Sony Headphones! Flat Rs. 3,000 discount today only. Click to buy."),
        ("Swiggy", "in.swiggy.android", "Hungry? 50% OFF just for you!", "Craving biryani? Use coupon code FEAST50 to get flat 50% discount up to Rs 100."),
        ("Zomato", "com.application.zomato", "Gold Members get Free Delivery!", "Order from top rated restaurants near you and enjoy zero delivery fee today!"),
        ("HDFC Bank", "com.snapwork.hdfc", "Pre-approved Credit Card", "Congratulations! You are eligible for Lifetime Free Regalia Credit Card with 10k bonus points."),
        ("Bajaj Finserv", "org.altruist.BajajExperia", "Instant Loan Approved", "Get Rs. 5 Lakhs personal loan credited in 5 minutes! No paperwork required."),
        ("Myntra", "com.myntra.android", "End of Reason Sale", "Extra 20% off on Nike, Adidas & Puma footwear. Hurry, stock selling fast!"),
        ("Tata Neu", "com.tatadigital.tcp", "NeuCoins Bonanza", "Earn 10% NeuCoins on groceries and electronics. Tap to claim."),
        ("Domino's Pizza", "com.pizza.dominos", "Buy 1 Get 1 Free Today!", "Cheesy Wednesday offer: Buy any medium pizza and get another free!"),
        ("Zepto", "com.zeptonow.android", "10-minute delivery discount", "Get fresh mangoes & veggies at flat 30% off. Delivery in 10 mins!"),
        ("Ajio", "com.ril.ajio", "Flash Sale starts in 1 hour", "Up to 80% off on international brands. Check preview now.")
    ]
    for app_name, pkg, title, body in promotions:
        for _ in range(8):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "PROMOTIONAL",
                "is_otp": False,
                "expected_priority": "P4"
            })

    # 8. GENERAL INFORMATIONAL
    general_info = [
        ("Google Weather", "com.google.android.googlequicksearchbox", "Weather Forecast", "Sunny and 29°C in New Delhi. Humidity 45%, light breeze."),
        ("Inshorts", "com.nis.app", "News Brief", "ISRO successfully launches next-gen communication satellite from Sriharikota."),
        ("BBC News", "bbc.mobile.news.ww", "Global Markets", "Asian stock markets steady following central bank interest rate announcement."),
        ("Fitbit", "com.fitbit.FitbitMobile", "Daily Step Count", "You walked 8,420 steps today. 1,580 steps left to hit your goal!"),
        ("Wikipedia", "org.wikipedia", "Did you know?", "Featured article: History of cryptography and one-time pads.")
    ]
    for app_name, pkg, title, body in general_info:
        for _ in range(6):
            records.append({
                "package_name": pkg,
                "application_name": app_name,
                "title": title,
                "body": body,
                "category": "GENERAL_INFORMATIONAL",
                "is_otp": False,
                "expected_priority": "P3"
            })

    random.seed(42)
    random.shuffle(records)
    return records


def main():
    data_dir = os.path.join(os.path.dirname(__file__), "..", "data")
    os.makedirs(data_dir, exist_ok=True)

    records = create_synthetic_data()
    df = pd.DataFrame(records)

    # Add text column for NLP
    df["full_text"] = df["title"].fillna("") + " - " + df["body"].fillna("")

    # Stratified Train-Test Split (80% train, 20% test)
    train_df, test_df = train_test_split(
        df, test_size=0.20, random_state=42, stratify=df["category"]
    )

    train_path = os.path.join(data_dir, "train_notifications.csv")
    test_path = os.path.join(data_dir, "test_notifications.csv")

    train_df.to_csv(train_path, index=False)
    test_df.to_csv(test_path, index=False)

    print(f"Dataset generated successfully!")
    print(f"Total samples: {len(df)}")
    print(f"Train samples: {len(train_df)} -> {train_path}")
    print(f"Test samples:  {len(test_df)} -> {test_path}")
    print("\nClass distribution in full dataset:")
    print(df["category"].value_counts())
    print(f"\nOTP count: {df['is_otp'].sum()}")


if __name__ == "__main__":
    main()
