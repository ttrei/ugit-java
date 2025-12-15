{
  description = "ugit";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    gradle2nix.url = "github:tadfisher/gradle2nix/v2";
  };
  outputs = { self, nixpkgs, gradle2nix }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
    in {
      packages.${system}.default =
        gradle2nix.builders.${system}.buildGradlePackage {
          pname = "ugit";
          version = "0.1";

          src = ./.;
          # generated with 'nix run github:tadfisher/gradle2nix/v2'
          lockFile = ./gradle.lock;

          gradleBuildFlags = [ "shadowJar" ];

          installPhase = ''
            mkdir -p $out/bin $out/lib
            cp build/libs/*-all.jar $out/lib/app.jar

            cat > $out/bin/ugit <<EOF
            #!${pkgs.bash}/bin/bash
            exec ${pkgs.jre}/bin/java -jar $out/lib/app.jar "\$@"
            EOF
            chmod +x $out/bin/ugit
          '';
        };

      devShells.default = pkgs.mkShell {
        buildInputs = with pkgs; [
          jdk21
          gradle
          graphviz
        ];

        shellHook = ''
          echo "ugit development environment"
          echo "Java version: $(java -version 2>&1 | head -n 1)"
        '';
      };
    };

}
