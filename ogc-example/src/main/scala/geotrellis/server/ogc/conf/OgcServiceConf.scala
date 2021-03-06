package geotrellis.server.ogc.conf

import geotrellis.server.ogc.ows
import geotrellis.server.ogc.wms.WmsParentLayerMeta
import geotrellis.server.ogc.{OgcSource, SimpleSource}
import geotrellis.server.ogc.wmts.GeotrellisTileMatrixSet

/**
 * Each service has its own unique configuration requirements (see the below instances)
 *  but share certain basic behaviors related to layer management. This trait encodes
 *  those expectations
 */
sealed trait OgcServiceConf {
  def layerDefinitions: List[OgcSourceConf]
  def layerSources(simpleSources: List[SimpleSource]): List[OgcSource] = {
    val simpleLayers =
      layerDefinitions.collect { case ssc@SimpleSourceConf(_, _, _, _) => ssc.model }
    val mapAlgebraLayers =
      layerDefinitions.collect { case masc@MapAlgebraSourceConf(_, _, _, _) => masc.model(simpleSources) }
    simpleLayers ++ mapAlgebraLayers
  }
}

/** WMS Service configuration */
case class WmsConf(
  parentLayerMeta: WmsParentLayerMeta,
  serviceMetadata: opengis.wms.Service,
  layerDefinitions: List[OgcSourceConf]
) extends OgcServiceConf

/** WMTS Service configuration */
case class WmtsConf(
  serviceMetadata: ows.ServiceMetadata,
  layerDefinitions: List[OgcSourceConf],
  tileMatrixSets: List[GeotrellisTileMatrixSet]
) extends OgcServiceConf

/** WCS Service configuration */
case class WcsConf(
  serviceMetadata: ows.ServiceMetadata,
  layerDefinitions: List[OgcSourceConf]
) extends OgcServiceConf

