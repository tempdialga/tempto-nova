package com.mygdx.tempto.maps;

import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.XmlReader;
import com.mygdx.tempto.data.CentralTextureData;

import org.lwjgl.Sys;

public class NewAtlasTmxMapLoader extends TmxMapLoader {

    WorldMap parent;

    public NewAtlasTmxMapLoader(LocalFileHandleResolver localFileHandleResolver) {
        super(localFileHandleResolver);
    }

    public NewAtlasTmxMapLoader() {
        super();
    }

    public void setParent(WorldMap parent) {
        this.parent = parent;
    }

    @Override
    protected AnimatedTiledMapTile createAnimatedTile (TiledMapTileSet tileSet, TiledMapTile tile, XmlReader.Element tileElement,
                                                       int firstgid) {
        XmlReader.Element animationElement = tileElement.getChildByName("animation");
        if (animationElement != null) {
            Array<StaticTiledMapTile> staticTiles = new Array<StaticTiledMapTile>();
            IntArray intervals = new IntArray();
            for (XmlReader.Element frameElement : animationElement.getChildrenByName("frame")) {
                staticTiles.add((StaticTiledMapTile)tileSet.getTile(firstgid + frameElement.getIntAttribute("tileid")));
                intervals.add(frameElement.getIntAttribute("duration"));
            }

            AnimatedTiledMapTile animatedTile = new AnimatedTiledMapTile(intervals, staticTiles);
            animatedTile.setId(tile.getId());
            String sourceCTD = (this.parent.pathDirFromData +tileSet.getProperties().get("imagesource",String.class)).replaceAll("\\.png$","");
            animatedTile.getProperties().put("sourceCTD",sourceCTD);
            return animatedTile;
        }
        return null;
    }

    @Override
    protected void addStaticTiles (FileHandle tmxFile, ImageResolver imageResolver, TiledMapTileSet tileSet, XmlReader.Element element,
                                   Array<XmlReader.Element> tileElements, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin,
                                   String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image) {
        //System.out.println("Adding tiles of name "+name);
        MapProperties props = tileSet.getProperties();
        if (image != null) {
            // One image for the whole tileSet

            props.put("imagesource", imageSource);
            props.put("imagewidth", imageWidth);
            props.put("imageheight", imageHeight);
            props.put("tilewidth", tilewidth);
            props.put("tileheight", tileheight);
            props.put("margin", margin);
            props.put("spacing", spacing);

            String sourceCTD = this.sourceToAtlasID(imageSource);
            TextureAtlas.AtlasRegion atlasRegion = CentralTextureData.getRegion(sourceCTD);
            TextureAtlas.AtlasRegion depthRegion = CentralTextureData.getDepthRegion(sourceCTD, true);

            int stopWidth = atlasRegion.getRegionWidth() - tilewidth;
            int stopHeight = atlasRegion.getRegionHeight() - tileheight;

            int id = firstgid;

            for (int y = margin; y <= stopHeight; y += tileheight + spacing) {
                for (int x = margin; x <= stopWidth; x += tilewidth + spacing) {
                    TextureRegion tileRegion = new TextureRegion(atlasRegion, x, y, tilewidth, tileheight);
                    TextureRegion tileDepthRegion;
                    if (depthRegion == CentralTextureData.defaultDepthRegion) {
                        tileDepthRegion = depthRegion;
                    } else {
                        tileDepthRegion = new TextureRegion(depthRegion, x, y, tilewidth, tileheight);
                    }
                    CentralTextureData.baseToDepthPairs.put(tileRegion, tileDepthRegion);

                    int tileId = id++;
                    addStaticTiledMapTile(tileSet, tileRegion, tileId, offsetX, offsetY, sourceCTD);
                }
            }
        } else {
            // Every tile has its own image source
            for (XmlReader.Element tileElement : tileElements) {
                XmlReader.Element imageElement = tileElement.getChildByName("image");
                if (imageElement != null) {
                    imageSource = imageElement.getAttribute("source");
                    String sourceCTD = this.sourceToAtlasID(imageSource);
                    TextureAtlas.AtlasRegion atlasRegion = CentralTextureData.getRegion(sourceCTD);
                    TextureAtlas.AtlasRegion depthRegion = CentralTextureData.getDepthRegion(sourceCTD, true);

                    CentralTextureData.baseToDepthPairs.put(atlasRegion, depthRegion);


                    int tileId = firstgid + tileElement.getIntAttribute("id");
                    addStaticTiledMapTile(tileSet, atlasRegion, tileId, offsetX, offsetY, sourceCTD);
                    continue;

//                    if (source != null) {
//                        image = getRelativeFileHandle(getRelativeFileHandle(tmxFile, source), imageSource);
//                    } else {
//                        image = getRelativeFileHandle(tmxFile, imageSource);
//                    }
                }
                TextureRegion texture = imageResolver.getImage(image.path());
                int tileId = firstgid + tileElement.getIntAttribute("id");
                addStaticTiledMapTile(tileSet, texture, tileId, offsetX, offsetY);
            }
        }
    }


    protected void addStaticTiledMapTile (TiledMapTileSet tileSet, TextureRegion textureRegion, int tileId, float offsetX,
                                          float offsetY, String sourceCTD) {
        TiledMapTile tile = new StaticTiledMapTile(textureRegion);
        tile.setId(tileId);
        tile.setOffsetX(offsetX);
        tile.setOffsetY(flipY ? -offsetY : offsetY);
        //String sourceCTD = this.sourceToAtlasID(tileSet.getProperties().get("imagesource",String.class));
        tile.getProperties().put("sourceCTD",sourceCTD);
        tileSet.putTile(tileId, tile);
    }

    /**Returns the probably texture atlas id in {@link com.mygdx.tempto.data.CentralTextureData} corresponding to the given tileset image source
     * @param tilesetImageSource the original png path source of the tileset, relative to the 'data/maps/' local directory*/
    protected String sourceToAtlasID(String tilesetImageSource) {
        return (this.parent.pathDirFromData+tilesetImageSource).replaceAll("\\.png$","");
    }

    /**Returns the depth region corresponding to the given CentralTextureData atlas ID. If there isn't one found, returns the default of */
}
