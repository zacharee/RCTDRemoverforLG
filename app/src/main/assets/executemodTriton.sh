#!/system/bin/sh

if [ ! -f /storage/emulated/0/AndroidImageKitchen/boot.img ]
then
    dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img
else
    cp /storage/emulated/0/AndroidImageKitchen/boot.img /data/local/AIK-mobile/boot.img
fi

./unpackimg.sh boot.img

cd ramdisk

sed -i -e '/# triton service/,\_chmod 644 /sys/devices/system/cpu/triton/enable_s/^/# /' init.elsa.power.rc

cd ../

./repackimg.sh

mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img