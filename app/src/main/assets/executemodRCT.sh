#!/system/bin/sh

dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img

./unpackimg.sh boot.img

cd ramdisk

sed -i -e '/^# LG RCT(Rooting Check Tool)$/,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc
sed -i -e '/^# service ccmd /system/bin/ccmd$/,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc

cd ../

./repackimg.sh

mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img