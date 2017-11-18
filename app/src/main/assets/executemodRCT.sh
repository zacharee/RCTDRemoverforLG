#!/system/bin/sh

if [ ! -f /storage/emulated/0/AndroidImageKitchen/boot.img ]
then
    dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img
else
    cp /storage/emulated/0/AndroidImageKitchen/boot.img /data/local/AIK-mobile/boot.img
fi

./unpackimg.sh boot.img

cd ramdisk

sed -i -e '/^# LG RCT(Rooting Check Tool)$/,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc
sed -i -e '\_^service ccmd /system/bin/ccmd$_,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc

cd ../

./repackimg.sh

mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img