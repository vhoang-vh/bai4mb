# Ứng dụng Quản lý Sinh viên

Ứng dụng Android viết bằng Kotlin và Jetpack Compose, lưu dữ liệu sinh viên
trên thiết bị bằng Room (SQLite).

## Chức năng

- Xem danh sách và thông tin chi tiết sinh viên.
- Thêm, sửa và xóa hồ sơ; xác nhận trước khi sửa hoặc xóa.
- Lưu họ tên, mã số sinh viên, email và ảnh đại diện.
- Chọn ảnh từ thiết bị hoặc nhập đường dẫn ảnh.
- Tự động hiển thị tiếng Việt hoặc tiếng Anh theo ngôn ngữ thiết bị.

## Chạy ứng dụng

1. Mở dự án bằng Android Studio.
2. Chờ Gradle đồng bộ, sau đó chạy cấu hình `app` trên máy ảo hoặc thiết bị
   Android.

Hoặc từ thư mục dự án chạy:

```powershell
.\gradlew.bat assembleDebug
```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## Công nghệ

- Kotlin, Jetpack Compose và Navigation Compose.
- Room Database với SQLite.
- Coil để tải ảnh đại diện.
