# 📍 TrackMate – Real-time Employee Location Tracking & Deployment App

**TrackMate** is a real-time employee location tracking and task dispatching solution designed for **service-based businesses** like ISPs (Internet Service Providers), field technicians, maintenance services, and more. It ensures efficient team coordination, streamlined deployment, and enhanced communication between employees and administrators.

---

## 🔧 Key Features

### 1. 👤 User Authentication and Profile Management
- Secure login via **Firebase Authentication**
- Editable user profiles: name, phone number, and profile photo
- Working status toggles (e.g., Free, Busy, In Meeting)
- Special **admin role** with elevated privileges

### 2. 📍 Real-time Location Sharing
- Live location tracking and sharing between connected users
- Visibility of each user’s working status and online/offline indicator
- Follow/unfollow system for selective location sharing

### 3. ✉️ Invitation and Team Management
- Email-based invitations to connect with team members
- Accept or reject connection requests
- Auto-connect feature: Admins can automatically link new users with existing team members
- Clear follower/following relationship tracking

### 4. 🗺️ Interactive Map Interface
- **Google Maps** integration to display live locations
- Color-coded user markers based on status (Free, Busy, etc.)
- Online/offline indicators for team visibility
- Select any location on the map for task assignment

### 5. ✅ Task Management System
- Admins can assign location-specific tasks with client details
- Workflow: `Pending → Accepted/Rejected → Completed`
- Auto route calculation using **Google Directions API**
- Real-time task tracking and completion updates

### 6. 📞 Integrated Communication Tools
- In-app **video/audio calling** using **ZegoCloud**
- Quick dial for client communication
- Communication support tied directly to task details

### 7. 🔐 Permissions and Security
- Controlled permissions for location, camera, microphone
- **Permission queue** to handle multiple permission dialogs
- Firebase-secured data management

### 8. 🔄 Real-time Synchronization
- Firestore listeners for instant data propagation
- Working status and location sync across all connected users
- **Background notification service** via Node.js and JavaScript deployed on Google Cloud
- FCM (Firebase Cloud Messaging) support for push notifications

### 9. 📱 Intuitive Navigation and UI
- Bottom navigation bar: Search, Map, Profile
- **Material Design**-based UI
- Modular dialog interfaces for tasks and invitations
- Responsive layout optimized for all screen sizes

### 10. 🛠️ Special Admin Functionalities
- Admin-exclusive task assignment system
- Full team visibility and status overview
- Automatic team onboarding for new users

---

## 🚀 Ideal Use Cases
- Internet Service Providers (ISP)
- Electrical & plumbing services
- Field repair and maintenance teams
- Logistics & delivery staff coordination

---

## 🧰 Tech Stack
- **Kotlin** (Android)
- **Firebase** (Auth, Firestore, Storage, FCM)
- **ZegoCloud** (Voice & Video Calls)
- **Google Maps SDK** & **Directions API**
- **JavaScript** & **Node.js** (Notification server code deployed on Google Cloud)

---

## 📌 Status
> 🟢 **TrackMate is under active development** with continuous feature enhancements and performance improvements.

---

## 📬 Contact
For questions, suggestions, or collaboration inquiries, feel free to reach out at:
- **Email**: [sagarmoktan2059@gmail.com]
- **GitHub**: [github.com/sagarmoktan20](https://github.com/sagarmoktan20)

---

**Empower your service team with smarter deployment. Try TrackMate today!**
