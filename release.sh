export KEEP_META=false
rm -Rf target/release
mkdir target/release
cp target/libclojure-objc.a target/release/
cp $J2OBJC_HOME/lib/libjre_emul.a target/release/
cp src/ffi/libffi.a target/release/
mkdir target/release/include
rsync -a $J2OBJC_HOME/include target/release
rsync -a target/include target/release
cp $J2OBJC_HOME/j2objc target/release/
mkdir target/release/lib
cp $J2OBJC_HOME/lib/j2objc_annotations.jar target/release/lib
cp $J2OBJC_HOME/lib/j2objc.jar target/release/lib
cp $J2OBJC_HOME/lib/jre_emul.jar target/release/lib
cd ./target/release
zip -r ../release.zip .
