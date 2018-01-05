#!/system/bin/sh

REM_RCTD=$1
REM_CCMD=$2
REM_TRITON=$3

dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img

./unpackimg.sh boot.img

cd ramdisk

if [ "$REM_RCTD" = "true" ];
then
    echo "Removing RCTD..."
    sed -i -e '/^# LG RCT(Rooting Check Tool)$/,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc
fi

if [ "$REM_CCMD" = "true" ];
then
    echo "Removing CCMD..."
    sed -i -e '\_^service ccmd /system/bin/ccmd$_,/^$/{/^\(#\|$\)/!s/^/#/}' init.lge.rc
fi

if [ "$REM_TRITON" = "true" ];
then
    echo "Removing Triton..."
    sed -i -e '/# triton service/,\_chmod 644 /sys/devices/system/cpu/triton/enable_s/^/# /' init.elsa.power.rc
fi

cd ../

./repackimg.sh

mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img