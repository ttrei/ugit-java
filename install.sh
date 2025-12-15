#!/usr/bin/env sh

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")

mkdir -p ~/bin
cat > ~/bin/ugit <<EOF
#!/usr/bin/env sh
java -jar "$SCRIPT_DIR"/build/libs/*.jar "\$@"
EOF
chmod +x ~/bin/ugit
