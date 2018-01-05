#!/system/bin/sh

REM_RCTD=$1
REM_CCMD=$2
REM_TRITON=$3

echo "<font color='#00ff00'>Copying boot.img from device...</font>"
dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img || exit 1

echo "<font color='#00ff00'>Extracting boot.img...</font>"
./unpackimg.sh boot.img || exit 1

cd ramdisk || exit 1

if [ "$REM_RCTD" = "true" ]
then
    echo "<font color='#00ff00'>Removing RCTD...</font>"
    sed -i -e '/^# LG RCT(Rooting Check Tool)$/,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc || exit 1
fi

if [ "$REM_CCMD" = "true" ]
then
    echo "<font color='#00ff00'>Removing CCMD...</font>"
    sed -i -e '\_^service ccmd /system/bin/ccmd$_,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc || exit 1
fi

if [ "$REM_TRITON" = "true" ]
then
    echo "<font color='#00ff00'>Removing Triton...</font>"
    sed -i -e '/# triton service/,\_chmod 644 /sys/devices/system/cpu/triton/enable_s/^/# /' init.elsa.power.rc || exit 1
fi

cd ../ || exit 1

echo "<font color='#00ff00'>Repacking boot.img...</font>"
./repackimg.sh || exit 1

echo "<font color='#00ff00'>Moving modified boot.img to /sdcard/AndroidImageKitchen/boot.img...</font>"
mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img || exit 1

echo "<font color='#00ff00'>Cleaning up...</font>"
./cleanup.sh

echo "Done!"
