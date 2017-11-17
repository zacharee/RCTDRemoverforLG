#!/system/bin/sh

dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img

./unpackimg.sh boot.img

cd ramdisk

sed -i -e '/# triton service/,\_chmod 644 /sys/devices/system/cpu/triton/enable_s/^/# /' init.lge.rc

cd ../

./repackimg.sh

mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img