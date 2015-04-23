java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.LeadTailEmptyDayDeleter %1 %2
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.EmptyDayFinder %1 %2
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.EmptyObservationFinder %1 %2
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.ObservationMerger %1 %2
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.ObservationWaypointMerger %1 %2
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.ObservationDeleter %1 %2
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.PositionObservationDeleter %1 %2

java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.WaypointBboxChecker %1 %2 xmin ymin xmax ymax
java -cp %CP1%;%CP2% org.wcs.smart.data.transforms.PatrolDateChecker %1 %2 30 01-01-1980
