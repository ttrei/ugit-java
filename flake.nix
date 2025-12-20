{
  description = "ugit";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };
  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
    in {

      devShells.${system}.default = pkgs.mkShell {
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
