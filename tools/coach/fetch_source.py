"""Fetch pinned CC0 inputs; generated models contain no network dependencies."""
import pathlib, urllib.request, sys
revision='a8bc2d54ff0ac92e78ff71431b1023eda42bf482'
base=f'https://raw.githubusercontent.com/makehumancommunity/makehuman/{revision}/makehuman/data/'
dest=pathlib.Path(sys.argv[1]);dest.mkdir(parents=True,exist_ok=True)
files={'base.obj':'3dobjs/base.obj','rig.json':'rigs/default.mhskel','weights.json':'rigs/default_weights.mhw'}
for name in ['caucasian-male-young.target','universal-male-young-averagemuscle-averageweight.target']:
    files[name]='targets/macrodetails/'+name
for name,url in files.items():
    (dest/name).write_bytes(urllib.request.urlopen(base+url).read())
print('Downloaded pinned MakeHuman CC0 mesh, targets and rig.')
