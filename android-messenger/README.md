# AIC School Messenger

Small Android companion for the AIC ERP. It polls the existing Supabase SMS queue and uses the Android phone's SIM to send ordinary SMS.

## First test

1. Install the debug APK on the school's Android phone.
2. Open AIC School Messenger.
3. Grant **SMS** permission.
4. Enter the device token supplied for this school device.
5. Tap **Save & Test Connection**.
6. The ERP creates an SMS broadcast with channel `sms` and status `phone_pending`.
7. The app checks the queue, shows the recipients, then sends the messages through the phone's SIM.

The first version intentionally limits each ERP broadcast to 50 recipients. It is a direct-install APK, not a Play Store app.
