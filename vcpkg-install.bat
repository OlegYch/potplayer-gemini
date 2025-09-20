git clone https://github.com/microsoft/vcpkg.git
cd vcpkg & ^
git checkout 7213cf8135c329c37c7e2778e40774489a0583a8 & ^
bootstrap-vcpkg.bat & ^
cd .. & ^
vcpkg\vcpkg.exe install
