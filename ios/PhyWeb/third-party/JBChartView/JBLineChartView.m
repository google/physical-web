//
//  JBLineChartView.m
//  Nudge
//
//  Created by Terry Worona on 9/4/13.
//  Copyright (c) 2013 Jawbone. All rights reserved.
//

#import "JBLineChartView.h"

// Drawing
#import <QuartzCore/QuartzCore.h>

// Enums
typedef NS_ENUM(NSUInteger, JBLineChartHorizontalIndexClamp){
	JBLineChartHorizontalIndexClampLeft,
    JBLineChartHorizontalIndexClampRight,
    JBLineChartHorizontalIndexClampNone
};

// Numerics (JBLineChartLineView)
CGFloat static const kJBLineChartLinesViewStrokeWidth = 5.0;
CGFloat static const kJBLineChartLinesViewMiterLimit = -5.0;
CGFloat static const kJBLineChartLinesViewDefaultLinePhase = 1.0f;
CGFloat static const kJBLineChartLinesViewDefaultDimmedOpacity = 0.20f;
NSInteger static const kJBLineChartLinesViewUnselectedLineIndex = -1;
CGFloat static const kJBLineChartLinesViewSmoothThresholdSlope = 0.01f;
NSInteger static const kJBLineChartLinesViewSmoothThresholdVertical = 1;

// Numerics (JBLineChartDotsView)
NSInteger static const kJBLineChartDotsViewDefaultRadiusFactor = 3; // 3x size of line width
NSInteger static const kJBLineChartDotsViewUnselectedLineIndex = -1;

// Numerics (JBLineSelectionView)
CGFloat static const kJBLineSelectionViewWidth = 20.0f;

// Numerics (JBLineChartView)
CGFloat static const kJBBarChartViewUndefinedCachedHeight = -1.0f;
CGFloat static const kJBLineChartViewStateAnimationDuration = 0.25f;
CGFloat static const kJBLineChartViewStateAnimationDelay = 0.05f;
CGFloat static const kJBLineChartViewStateBounceOffset = 15.0f;
NSInteger static const kJBLineChartUnselectedLineIndex = -1;

// Collections (JBLineChartLineView)
static NSArray *kJBLineChartLineViewDefaultDashPattern = nil;

// Colors (JBLineChartView)
static UIColor *kJBLineChartViewDefaultLineColor = nil;
static UIColor *kJBLineChartViewDefaultLineFillColor = nil;
static UIColor *kJBLineChartViewDefaultLineSelectionColor = nil;
static UIColor *kJBLineChartViewDefaultLineSelectionFillColor = nil;
static UIColor *kJBLineChartViewDefaultDotColor = nil;
static UIColor *kJBLineChartViewDefaultDotSelectionColor = nil;

@interface JBChartView (Private)

- (BOOL)hasMaximumValue;
- (BOOL)hasMinimumValue;

@end

@interface JBLineLayer : CAShapeLayer

@property (nonatomic, assign) NSUInteger tag;
@property (nonatomic, assign) JBLineChartViewLineStyle lineStyle;

@end

@interface JBFillLayer : CAShapeLayer

@property (nonatomic, assign) NSUInteger tag;

@end

@interface JBLineChartPoint : NSObject

@property (nonatomic, assign) CGPoint position;
@property (nonatomic, assign) BOOL hidden;

@end

@protocol JBLineChartLinesViewDelegate;

@interface JBLineChartLinesView : UIView

@property (nonatomic, assign) id<JBLineChartLinesViewDelegate> delegate;
@property (nonatomic, assign) NSInteger selectedLineIndex; // -1 to unselect
@property (nonatomic, assign) BOOL animated;

// Data
- (void)reloadData;

// Setters
- (void)setSelectedLineIndex:(NSInteger)selectedLineIndex animated:(BOOL)animated;

// Callback helpers
- (void)fireCallback:(void (^)())callback;

// View helpers
- (JBLineLayer *)lineLayerForLineIndex:(NSUInteger)lineIndex;
- (JBFillLayer *)fillLayerForLineIndex:(NSUInteger)lineIndex;

@end

@protocol JBLineChartLinesViewDelegate <NSObject>

- (NSArray *)chartDataForLineChartLinesView:(JBLineChartLinesView*)lineChartLinesView;
- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView colorForLineAtLineIndex:(NSUInteger)lineIndex;
- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView selectedColorForLineAtLineIndex:(NSUInteger)lineIndex;
- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView fillColorForLineAtLineIndex:(NSUInteger)lineIndex;
- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView selectedFillColorForLineAtLineIndex:(NSUInteger)lineIndex;
- (CGFloat)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView widthForLineAtLineIndex:(NSUInteger)lineIndex;
- (CGFloat)paddingForLineChartLinesView:(JBLineChartLinesView *)lineChartLinesView;
- (JBLineChartViewLineStyle)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView lineStyleForLineAtLineIndex:(NSUInteger)lineIndex;
- (BOOL)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView smoothLineAtLineIndex:(NSUInteger)lineIndex;

@end

@protocol JBLineChartDotsViewDelegate;

@interface JBLineChartDotsView : UIView // JBLineChartViewLineStyleDotted

@property (nonatomic, assign) id<JBLineChartDotsViewDelegate> delegate;
@property (nonatomic, assign) NSInteger selectedLineIndex; // -1 to unselect
@property (nonatomic, strong) NSDictionary *dotViewsDict;

// Data
- (void)reloadData;

// Setters
- (void)setSelectedLineIndex:(NSInteger)selectedLineIndex animated:(BOOL)animated;

@end

@protocol JBLineChartDotsViewDelegate <NSObject>

- (NSArray *)chartDataForLineChartDotsView:(JBLineChartDotsView*)lineChartDotsView;
- (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView colorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex;
- (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView selectedColorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex;
- (CGFloat)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView widthForLineAtLineIndex:(NSUInteger)lineIndex;
- (CGFloat)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView dotRadiusForLineAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex;
- (UIView *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView dotViewAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex;
- (BOOL)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView shouldHideDotViewOnSelectionAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex;
- (CGFloat)paddingForLineChartDotsView:(JBLineChartDotsView *)lineChartDotsView;
- (BOOL)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView showsDotsForLineAtLineIndex:(NSUInteger)lineIndex;

@end

@interface JBLineChartDotView : UIView

- (id)initWithRadius:(CGFloat)radius;

@end

@interface JBLineChartView () <JBLineChartLinesViewDelegate, JBLineChartDotsViewDelegate>

@property (nonatomic, strong) NSArray *chartData;
@property (nonatomic, strong) JBLineChartLinesView *linesView;
@property (nonatomic, strong) JBLineChartDotsView *dotsView;
@property (nonatomic, strong) JBChartVerticalSelectionView *verticalSelectionView;
@property (nonatomic, assign) CGFloat cachedMaxHeight;
@property (nonatomic, assign) CGFloat cachedMinHeight;
@property (nonatomic, assign) BOOL verticalSelectionViewVisible;

// Initialization
- (void)construct;

// View quick accessors
- (CGFloat)normalizedHeightForRawHeight:(CGFloat)rawHeight;
- (CGFloat)availableHeight;
- (CGFloat)padding;
- (NSUInteger)dataCount;

// Touch helpers
- (CGPoint)clampPoint:(CGPoint)point toBounds:(CGRect)bounds padding:(CGFloat)padding;
- (NSInteger)horizontalIndexForPoint:(CGPoint)point indexClamp:(JBLineChartHorizontalIndexClamp)indexClamp lineData:(NSArray *)lineData;
- (NSInteger)horizontalIndexForPoint:(CGPoint)point indexClamp:(JBLineChartHorizontalIndexClamp)indexClamp; // uses largest line data
- (NSInteger)horizontalIndexForPoint:(CGPoint)point;
- (NSInteger)lineIndexForPoint:(CGPoint)point;
- (void)touchesBeganOrMovedWithTouches:(NSSet *)touches;
- (void)touchesEndedOrCancelledWithTouches:(NSSet *)touches;

// Setters
- (void)setVerticalSelectionViewVisible:(BOOL)verticalSelectionViewVisible animated:(BOOL)animated;

@end

@implementation JBLineChartView

@dynamic dataSource;
@dynamic delegate;

#pragma mark - Alloc/Init

+ (void)initialize
{
	if (self == [JBLineChartView class])
	{
		kJBLineChartViewDefaultLineColor = [UIColor blackColor];
        kJBLineChartViewDefaultLineFillColor = [UIColor clearColor];
		kJBLineChartViewDefaultLineSelectionColor = [UIColor whiteColor];
        kJBLineChartViewDefaultLineSelectionFillColor = [UIColor clearColor];
        kJBLineChartViewDefaultDotColor = [UIColor blackColor];
        kJBLineChartViewDefaultDotSelectionColor = [UIColor whiteColor];
	}
}

- (id)initWithCoder:(NSCoder *)aDecoder
{
    self = [super initWithCoder:aDecoder];
    if (self)
    {
        [self construct];
    }
    return self;
}

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self)
    {
        [self construct];
    }
    return self;
}

- (id)init
{
    self = [super init];
    if (self)
    {
        [self construct];
    }
    return self;
}

- (void)construct
{
    _showsVerticalSelection = YES;
    _showsLineSelection = YES;
    _cachedMinHeight = kJBBarChartViewUndefinedCachedHeight;
    _cachedMaxHeight = kJBBarChartViewUndefinedCachedHeight;
}

#pragma mark - Data

- (void)reloadData
{
    // Reset cached max height
    self.cachedMinHeight = kJBBarChartViewUndefinedCachedHeight;
    self.cachedMaxHeight = kJBBarChartViewUndefinedCachedHeight;
    
    // Padding
    CGFloat chartPadding = [self padding];

    /*
     * Subview rectangle calculations
     */
    CGRect mainViewRect = CGRectMake(self.bounds.origin.x, self.bounds.origin.y, self.bounds.size.width, [self availableHeight]);

    /*
     * The data collection holds all position and marker information:
     * constructed via datasource and delegate functions
     */
    dispatch_block_t createChartData = ^{

        CGFloat pointSpace = (self.bounds.size.width - (chartPadding * 2)) / ([self dataCount] - 1); // Space in between points
        CGFloat xOffset = chartPadding;
        CGFloat yOffset = 0;

        NSMutableArray *mutableChartData = [NSMutableArray array];
        NSAssert([self.dataSource respondsToSelector:@selector(numberOfLinesInLineChartView:)], @"JBLineChartView // dataSource must implement - (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView");
        NSUInteger numberOfLines = [self.dataSource numberOfLinesInLineChartView:self];
        for (NSUInteger lineIndex=0; lineIndex<numberOfLines; lineIndex++)
        {
            NSAssert([self.dataSource respondsToSelector:@selector(lineChartView:numberOfVerticalValuesAtLineIndex:)], @"JBLineChartView // dataSource must implement - (NSUInteger)lineChartView:(JBLineChartView *)lineChartView numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex");
            NSUInteger dataCount = [self.dataSource lineChartView:self numberOfVerticalValuesAtLineIndex:lineIndex];
            NSMutableArray *chartPointData = [NSMutableArray array];
            for (NSUInteger horizontalIndex=0; horizontalIndex<dataCount; horizontalIndex++)
            {               
                NSAssert([self.delegate respondsToSelector:@selector(lineChartView:verticalValueForHorizontalIndex:atLineIndex:)], @"JBLineChartView // delegate must implement - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                CGFloat rawHeight =  [self.delegate lineChartView:self verticalValueForHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                NSAssert(isnan(rawHeight) || (rawHeight >= 0), @"JBLineChartView // delegate function - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex must return a CGFloat >= 0 OR NAN");

                JBLineChartPoint *chartPoint = [[JBLineChartPoint alloc] init];
                if(isnan(rawHeight))
                {
                    chartPoint.hidden = YES;
                    rawHeight = 0; //set to 0 so we can calculate the x position
                }

                CGFloat normalizedHeight = [self normalizedHeightForRawHeight:rawHeight];
                yOffset = mainViewRect.size.height - normalizedHeight;
                
                chartPoint.position = CGPointMake(xOffset, yOffset);

                [chartPointData addObject:chartPoint];
                xOffset += pointSpace;
            }
            [mutableChartData addObject:chartPointData];
            xOffset = chartPadding;
        }
        self.chartData = [NSArray arrayWithArray:mutableChartData];
	};

    /*
     * Creates a new line graph view using the previously calculated data model
     */
    dispatch_block_t createLineGraphView = ^{

        // Remove old line view
        if (self.linesView)
        {
            self.linesView.delegate = nil;
            [self.linesView removeFromSuperview];
            self.linesView = nil;
        }

        // Create new line and overlay subviews
        self.linesView = [[JBLineChartLinesView alloc] initWithFrame:CGRectOffset(mainViewRect, 0, self.headerView.frame.size.height + self.headerPadding)];
        self.linesView.delegate = self;

        // Add new lines view
        if (self.footerView)
        {
            [self insertSubview:self.linesView belowSubview:self.footerView];
        }
        else
        {
            [self addSubview:self.linesView];
        }
    };

    /*
     * Creates a new dot graph view using the previously calculated data model
     */
    dispatch_block_t createDotGraphView = ^{
        
        // Remove old dot view
        if (self.dotsView)
        {
            self.dotsView.delegate = nil;
            [self.dotsView removeFromSuperview];
            self.dotsView = nil;
        }
        
        // Create new line and overlay subviews
        self.dotsView = [[JBLineChartDotsView alloc] initWithFrame:CGRectOffset(mainViewRect, 0, self.headerView.frame.size.height + self.headerPadding)];
        self.dotsView.delegate = self;
        
        // Add new dots view
        if (self.footerView)
        {
            [self insertSubview:self.dotsView belowSubview:self.footerView];
        }
        else
        {
            [self addSubview:self.dotsView];
        }
    };
    
    /*
     * Creates a vertical selection view for touch events
     */
    dispatch_block_t createSelectionView = ^{
        if (self.verticalSelectionView)
        {
            [self.verticalSelectionView removeFromSuperview];
            self.verticalSelectionView = nil;
        }

        CGFloat selectionViewWidth = kJBLineSelectionViewWidth;
        if ([self.delegate respondsToSelector:@selector(verticalSelectionWidthForLineChartView:)])
        {
            selectionViewWidth = MIN([self.delegate verticalSelectionWidthForLineChartView:self], self.bounds.size.width);
        }
        
        CGFloat verticalSelectionViewHeight = self.bounds.size.height - self.headerView.frame.size.height - self.footerView.frame.size.height - self.headerPadding - self.footerPadding;
        
        if ([self.dataSource respondsToSelector:@selector(shouldExtendSelectionViewIntoHeaderPaddingForChartView:)])
        {
            if ([self.dataSource shouldExtendSelectionViewIntoHeaderPaddingForChartView:self])
            {
                verticalSelectionViewHeight += self.headerPadding;
            }
        }
        
        if ([self.dataSource respondsToSelector:@selector(shouldExtendSelectionViewIntoFooterPaddingForChartView:)])
        {
            if ([self.dataSource shouldExtendSelectionViewIntoFooterPaddingForChartView:self])
            {
                verticalSelectionViewHeight += self.footerPadding;
            }
        }

        self.verticalSelectionView = [[JBChartVerticalSelectionView alloc] initWithFrame:CGRectMake(0, 0, selectionViewWidth, verticalSelectionViewHeight)];
        self.verticalSelectionView.alpha = 0.0;
        self.verticalSelectionView.hidden = !self.showsVerticalSelection;

        // Add new selection bar
        if (self.footerView)
        {
            [self insertSubview:self.verticalSelectionView belowSubview:self.footerView];
        }
        else
        {
            [self addSubview:self.verticalSelectionView];
        }
    };

    createChartData();
    createLineGraphView();
    createDotGraphView();
    createSelectionView();

    // Reload views
    [self.linesView reloadData];
    [self.dotsView reloadData];

    // Position header and footer
    self.headerView.frame = CGRectMake(self.bounds.origin.x, self.bounds.origin.y, self.bounds.size.width, self.headerView.frame.size.height);
    self.footerView.frame = CGRectMake(self.bounds.origin.x, self.bounds.size.height - self.footerView.frame.size.height, self.bounds.size.width, self.footerView.frame.size.height);

    // Refresh state
    [self setState:self.state animated:NO force:YES callback:nil];
}

#pragma mark - View Quick Accessors

- (CGFloat)normalizedHeightForRawHeight:(CGFloat)rawHeight
{
    CGFloat minHeight = [self minimumValue];
    CGFloat maxHeight = [self maximumValue];

    if ((maxHeight - minHeight) <= 0)
    {
        return 0;
    }

    return ((rawHeight - minHeight) / (maxHeight - minHeight)) * [self availableHeight];
}

- (CGFloat)availableHeight
{
    return self.bounds.size.height - self.headerView.frame.size.height - self.footerView.frame.size.height - self.headerPadding - self.footerPadding;
}

- (CGFloat)padding
{
    CGFloat maxLineWidth = 0.0f;
    NSAssert([self.dataSource respondsToSelector:@selector(numberOfLinesInLineChartView:)], @"JBLineChartView // dataSource must implement - (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView");
    NSInteger numberOfLines = [self.dataSource numberOfLinesInLineChartView:self];

    for (NSInteger lineIndex=0; lineIndex<numberOfLines; lineIndex++)
    {
        BOOL showsDots = NO;
        if ([self.dataSource respondsToSelector:@selector(lineChartView:showsDotsForLineAtLineIndex:)])
        {
            showsDots = [self.dataSource lineChartView:self showsDotsForLineAtLineIndex:lineIndex];
        }

        CGFloat lineWidth = kJBLineChartLinesViewStrokeWidth; // default
        if ([self.delegate respondsToSelector:@selector(lineChartView:widthForLineAtLineIndex:)])
        {
            lineWidth = [self.delegate lineChartView:self widthForLineAtLineIndex:lineIndex];
        }
        
        CGFloat maxDotLength = 0;
        if (showsDots)
        {
            NSAssert([self.dataSource respondsToSelector:@selector(lineChartView:numberOfVerticalValuesAtLineIndex:)], @"JBLineChartView // dataSource must implement - (NSUInteger)lineChartView:(JBLineChartView *)lineChartView numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex");
            NSUInteger dataCount = [self.dataSource lineChartView:self numberOfVerticalValuesAtLineIndex:lineIndex];
            
            for (NSUInteger horizontalIndex=0; horizontalIndex<dataCount; horizontalIndex++)
            {
                BOOL shouldEvaluateDotSize = NO;
                
                // Left dot
                if (horizontalIndex == 0)
                {
                    shouldEvaluateDotSize = YES;
                }
                // Right dot
                else if (horizontalIndex == (dataCount - 1))
                {
                    shouldEvaluateDotSize = YES;
                }
                else
                {
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartView:verticalValueForHorizontalIndex:atLineIndex:)], @"JBLineChartView // delegate must implement - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                    CGFloat height = [self.delegate lineChartView:self verticalValueForHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                    
                    // Top
                    if (height == [self cachedMaxHeight])
                    {
                        shouldEvaluateDotSize = YES;
                    }
                    
                    // Bottom
                    else if (height == [self cachedMinHeight])
                    {
                        shouldEvaluateDotSize = YES;
                    }
                }
                
                if (shouldEvaluateDotSize)
                {
                    if ([self.dataSource respondsToSelector:@selector(lineChartView:dotViewAtHorizontalIndex:atLineIndex:)])
                    {
                        if ([self.dataSource respondsToSelector:@selector(lineChartView:dotViewAtHorizontalIndex:atLineIndex:)])
                        {
                            UIView *customDotView = [self.dataSource lineChartView:self dotViewAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                            if (customDotView.frame.size.width > maxDotLength || customDotView.frame.size.height > maxDotLength)
                            {
                                maxDotLength = fmaxf(customDotView.frame.size.width, customDotView.frame.size.height);
                            }
                        }
                    }
                    else if ([self.delegate respondsToSelector:@selector(lineChartView:dotRadiusForDotAtHorizontalIndex:atLineIndex:)])
                    {
						CGFloat dotRadius = ([self.delegate lineChartView:self dotRadiusForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex] * 2.0f);
                        if (dotRadius > maxDotLength)
                        {
                            maxDotLength = dotRadius;
                        }
                    }
                    else
                    {
						CGFloat defaultDotRadius = ((lineWidth * kJBLineChartDotsViewDefaultRadiusFactor) * 2.0f);
                        if (defaultDotRadius > maxDotLength)
                        {
                            maxDotLength = defaultDotRadius;
                        }
                    }
                }
            }
        }
        
        CGFloat currentMaxLineWidth = MAX(maxDotLength, lineWidth);
        if (currentMaxLineWidth > maxLineWidth)
        {
            maxLineWidth = currentMaxLineWidth;
        }
    }
    return ceil(maxLineWidth * 0.5);
}

- (NSUInteger)dataCount
{
    NSUInteger dataCount = 0;
    NSAssert([self.dataSource respondsToSelector:@selector(numberOfLinesInLineChartView:)], @"JBLineChartView // dataSource must implement - (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView");
    NSInteger numberOfLines = [self.dataSource numberOfLinesInLineChartView:self];
    for (NSInteger lineIndex=0; lineIndex<numberOfLines; lineIndex++)
    {
        NSAssert([self.dataSource respondsToSelector:@selector(lineChartView:numberOfVerticalValuesAtLineIndex:)], @"JBLineChartView // dataSource must implement - (NSUInteger)lineChartView:(JBLineChartView *)lineChartView numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex");
        NSUInteger lineDataCount = [self.dataSource lineChartView:self numberOfVerticalValuesAtLineIndex:lineIndex];
        if (lineDataCount > dataCount)
        {
            dataCount = lineDataCount;
        }
    }
    return dataCount;
}

#pragma mark - JBLineChartLinesViewDelegate

- (NSArray *)chartDataForLineChartLinesView:(JBLineChartLinesView *)lineChartLinesView
{
    return self.chartData;
}

- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView colorForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:colorForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self colorForLineAtLineIndex:lineIndex];
    }
    return kJBLineChartViewDefaultLineColor;
}

- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView selectedColorForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:selectionColorForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self selectionColorForLineAtLineIndex:lineIndex];
    }
    return kJBLineChartViewDefaultLineSelectionColor;
}

- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView fillColorForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:fillColorForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self fillColorForLineAtLineIndex:lineIndex];
    }
    return kJBLineChartViewDefaultLineFillColor;
}

- (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView selectedFillColorForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:selectionFillColorForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self selectionFillColorForLineAtLineIndex:lineIndex];
    }
    return kJBLineChartViewDefaultLineSelectionFillColor;
}

- (CGFloat)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView widthForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:widthForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self widthForLineAtLineIndex:lineIndex];
    }
    return kJBLineChartLinesViewStrokeWidth;
}

- (CGFloat)paddingForLineChartLinesView:(JBLineChartLinesView *)lineChartLinesView
{
    return [self padding];
}

- (JBLineChartViewLineStyle)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView lineStyleForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:lineStyleForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self lineStyleForLineAtLineIndex:lineIndex];
    }
    return JBLineChartViewLineStyleSolid;
}

- (BOOL)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView smoothLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.dataSource respondsToSelector:@selector(lineChartView:smoothLineAtLineIndex:)])
    {
        return [self.dataSource lineChartView:self smoothLineAtLineIndex:lineIndex];
    }
    return NO;
}

#pragma mark - JBLineChartDotsViewDelegate

- (NSArray *)chartDataForLineChartDotsView:(JBLineChartDotsView*)lineChartDotsView
{
    return self.chartData;
}

- (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView colorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:colorForDotAtHorizontalIndex:atLineIndex:)])
    {
        return [self.delegate lineChartView:self colorForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
    }
    return kJBLineChartViewDefaultDotColor;
}

- (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView selectedColorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:selectionColorForDotAtHorizontalIndex:atLineIndex:)])
    {
        return [self.delegate lineChartView:self selectionColorForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
    }
    return kJBLineChartViewDefaultDotSelectionColor;
}

- (CGFloat)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView widthForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:widthForLineAtLineIndex:)])
    {
        return [self.delegate lineChartView:self widthForLineAtLineIndex:lineIndex];
    }
    return kJBLineChartLinesViewStrokeWidth;
}

- (CGFloat)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView dotRadiusForLineAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex
{
    if ([self.delegate respondsToSelector:@selector(lineChartView:dotRadiusForDotAtHorizontalIndex:atLineIndex:)])
    {
        return [self.delegate lineChartView:self dotRadiusForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
    }
    return [self lineChartDotsView:lineChartDotsView widthForLineAtLineIndex:lineIndex] * kJBLineChartDotsViewDefaultRadiusFactor;
}

- (UIView *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView dotViewAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex
{
    if ([self.dataSource respondsToSelector:@selector(lineChartView:dotViewAtHorizontalIndex:atLineIndex:)])
    {
        return [self.dataSource lineChartView:self dotViewAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
    }
    return nil;
}

- (BOOL)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView shouldHideDotViewOnSelectionAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex
{
    if ([self.dataSource respondsToSelector:@selector(lineChartView:shouldHideDotViewOnSelectionAtHorizontalIndex:atLineIndex:)])
    {
        return [self.dataSource lineChartView:self shouldHideDotViewOnSelectionAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
    }
    return NO;
}

- (CGFloat)paddingForLineChartDotsView:(JBLineChartDotsView *)lineChartDotsView
{
    return [self padding];
}

- (BOOL)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView showsDotsForLineAtLineIndex:(NSUInteger)lineIndex
{
    if ([self.dataSource respondsToSelector:@selector(lineChartView:showsDotsForLineAtLineIndex:)])
    {
        return [self.dataSource lineChartView:self showsDotsForLineAtLineIndex:lineIndex];
    }
    return NO;
}

#pragma mark - Setters

- (void)setState:(JBChartViewState)state animated:(BOOL)animated force:(BOOL)force callback:(void (^)())callback
{
    [super setState:state animated:animated force:force callback:callback];
    
    if ([self.chartData count] > 0)
    {
        CGRect mainViewRect = CGRectMake(self.bounds.origin.x, self.bounds.origin.y, self.bounds.size.width, [self availableHeight]);
        CGFloat yOffset = self.headerView.frame.size.height + self.headerPadding;
        
        dispatch_block_t adjustViewFrames = ^{
            self.linesView.frame = CGRectMake(self.linesView.frame.origin.x, yOffset + ((self.state == JBChartViewStateCollapsed) ? (self.linesView.frame.size.height + self.footerView.frame.size.height) : 0.0), self.linesView.frame.size.width, self.linesView.frame.size.height);
            self.dotsView.frame = CGRectMake(self.dotsView.frame.origin.x, yOffset + ((self.state == JBChartViewStateCollapsed) ? (self.dotsView.frame.size.height + self.footerView.frame.size.height) : 0.0), self.dotsView.frame.size.width, self.dotsView.frame.size.height);
        };
        
        dispatch_block_t adjustViewAlphas = ^{
            self.linesView.alpha = (self.state == JBChartViewStateExpanded) ? 1.0 : 0.0;
            self.dotsView.alpha = (self.state == JBChartViewStateExpanded) ? 1.0 : 0.0;
        };
        
        if (animated)
        {
            [UIView animateWithDuration:(kJBLineChartViewStateAnimationDuration * 0.5) delay:0.0 options:UIViewAnimationOptionBeginFromCurrentState animations:^{
                self.linesView.frame = CGRectOffset(mainViewRect, 0, yOffset - kJBLineChartViewStateBounceOffset); // bounce
                self.dotsView.frame = CGRectOffset(mainViewRect, 0, yOffset - kJBLineChartViewStateBounceOffset);
            } completion:^(BOOL finished) {
                [UIView animateWithDuration:kJBLineChartViewStateAnimationDuration delay:0.0 options:UIViewAnimationOptionBeginFromCurrentState animations:^{
                    adjustViewFrames();
                } completion:^(BOOL adjustFinished) {
                    if (callback)
                    {
                        callback();
                    }
                }];
            }];
            [UIView animateWithDuration:kJBLineChartViewStateAnimationDuration delay:(self.state == JBChartViewStateExpanded) ? kJBLineChartViewStateAnimationDelay : 0.0 options:UIViewAnimationOptionBeginFromCurrentState animations:^{
                adjustViewAlphas();
            } completion:nil];
        }
        else
        {
            adjustViewAlphas();
            adjustViewFrames();
            if (callback)
            {
                callback();
            }
        }
    }
    else
    {
        if (callback)
        {
            callback();
        }
    }
}

- (void)setState:(JBChartViewState)state animated:(BOOL)animated callback:(void (^)())callback
{
    [self setState:state animated:animated force:NO callback:callback];
}

- (void)setVerticalSelectionViewVisible:(BOOL)verticalSelectionViewVisible animated:(BOOL)animated
{
	_verticalSelectionViewVisible = verticalSelectionViewVisible;
	
	if (animated)
	{
		[UIView animateWithDuration:kJBChartViewDefaultAnimationDuration delay:0.0 options:UIViewAnimationOptionBeginFromCurrentState animations:^{
			self.verticalSelectionView.alpha = self.verticalSelectionViewVisible ? 1.0 : 0.0;
		} completion:nil];
	}
	else
	{
		self.verticalSelectionView.alpha = _verticalSelectionViewVisible ? 1.0 : 0.0;
	}
}

- (void)setVerticalSelectionViewVisible:(BOOL)verticalSelectionViewVisible
{
	[self setVerticalSelectionViewVisible:verticalSelectionViewVisible animated:NO];
}

- (void)setShowsVerticalSelection:(BOOL)showsVerticalSelection
{
	_showsVerticalSelection = showsVerticalSelection;
	self.verticalSelectionView.hidden = _showsVerticalSelection ? NO : YES;
}

#pragma mark - Getters

- (CGFloat)cachedMinHeight
{
    if (_cachedMinHeight == kJBBarChartViewUndefinedCachedHeight)
    {
        CGFloat minHeight = FLT_MAX;
        NSAssert([self.dataSource respondsToSelector:@selector(numberOfLinesInLineChartView:)], @"JBLineChartView // dataSource must implement - (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView");
        NSUInteger numberOfLines = [self.dataSource numberOfLinesInLineChartView:self];
        for (NSUInteger lineIndex=0; lineIndex<numberOfLines; lineIndex++)
        {
            NSAssert([self.dataSource respondsToSelector:@selector(lineChartView:numberOfVerticalValuesAtLineIndex:)], @"JBLineChartView // dataSource must implement - (NSUInteger)lineChartView:(JBLineChartView *)lineChartView numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex");
            NSUInteger dataCount = [self.dataSource lineChartView:self numberOfVerticalValuesAtLineIndex:lineIndex];
            for (NSUInteger horizontalIndex=0; horizontalIndex<dataCount; horizontalIndex++)
            {
                NSAssert([self.delegate respondsToSelector:@selector(lineChartView:verticalValueForHorizontalIndex:atLineIndex:)], @"JBLineChartView // delegate must implement - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                CGFloat height = [self.delegate lineChartView:self verticalValueForHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                NSAssert(isnan(height) || (height >= 0), @"JBLineChartView // delegate function - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex must return a CGFloat >= 0 OR NAN");
                if (!isnan(height) && height < minHeight)
                {
                    minHeight = height;
                }
            }
        }
        _cachedMinHeight = minHeight;
    }
    return _cachedMinHeight;
}

- (CGFloat)cachedMaxHeight
{
    if (_cachedMaxHeight == kJBBarChartViewUndefinedCachedHeight)
    {
        CGFloat maxHeight = 0;
        NSAssert([self.dataSource respondsToSelector:@selector(numberOfLinesInLineChartView:)], @"JBLineChartView // dataSource must implement - (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView");
        NSUInteger numberOfLines = [self.dataSource numberOfLinesInLineChartView:self];
        for (NSUInteger lineIndex=0; lineIndex<numberOfLines; lineIndex++)
        {
            NSAssert([self.dataSource respondsToSelector:@selector(lineChartView:numberOfVerticalValuesAtLineIndex:)], @"JBLineChartView // dataSource must implement - (NSUInteger)lineChartView:(JBLineChartView *)lineChartView numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex");
            NSUInteger dataCount = [self.dataSource lineChartView:self numberOfVerticalValuesAtLineIndex:lineIndex];
            for (NSUInteger horizontalIndex=0; horizontalIndex<dataCount; horizontalIndex++)
            {
                NSAssert([self.delegate respondsToSelector:@selector(lineChartView:verticalValueForHorizontalIndex:atLineIndex:)], @"JBLineChartView // delegate must implement - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                CGFloat height = [self.delegate lineChartView:self verticalValueForHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                NSAssert(isnan(height) || (height >= 0), @"JBLineChartView // delegate function - (CGFloat)lineChartView:(JBLineChartView *)lineChartView verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex must return a CGFloat >= 0 OR NAN");
                if (!isnan(height) && height > maxHeight)
                {
                    maxHeight = height;
                }
            }
        }
        _cachedMaxHeight = maxHeight;
    }
    return _cachedMaxHeight;
}

- (CGFloat)minimumValue
{
    if ([self hasMinimumValue])
    {
        return [super minimumValue];
    }
    return self.cachedMinHeight;
}

- (CGFloat)maximumValue
{
    if ([self hasMaximumValue])
    {
        return [super maximumValue];
    }
    return self.cachedMaxHeight;
}

- (UIView *)dotViewAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex
{
	NSArray *dotViews = [self.dotsView.dotViewsDict objectForKey:@(lineIndex)];
	if (horizontalIndex < [dotViews count])
	{
		return [dotViews objectAtIndex:horizontalIndex];
	}
	return nil;
}

#pragma mark - Touch Helpers

- (CGPoint)clampPoint:(CGPoint)point toBounds:(CGRect)bounds padding:(CGFloat)padding
{
    return CGPointMake(MIN(MAX(bounds.origin.x + padding, point.x), bounds.size.width - padding),
                       MIN(MAX(bounds.origin.y + padding, point.y), bounds.size.height - padding));
}

- (NSInteger)horizontalIndexForPoint:(CGPoint)point indexClamp:(JBLineChartHorizontalIndexClamp)indexClamp lineData:(NSArray *)lineData
{
    NSUInteger index = 0;
    CGFloat currentDistance = INT_MAX;
    NSInteger selectedIndex = kJBLineChartUnselectedLineIndex;
    
    for (JBLineChartPoint *lineChartPoint in lineData)
    {
        BOOL clamped = (indexClamp == JBLineChartHorizontalIndexClampNone) ? YES : (indexClamp == JBLineChartHorizontalIndexClampLeft) ? (point.x - lineChartPoint.position.x >= 0) : (point.x - lineChartPoint.position.x <= 0);
        if ((fabs(point.x - lineChartPoint.position.x)) < currentDistance && clamped == YES)
        {
            currentDistance = (fabs(point.x - lineChartPoint.position.x));
            selectedIndex = index;
        }
        index++;
    }
    
    return selectedIndex != kJBLineChartUnselectedLineIndex ? selectedIndex : [lineData count] - 1;
}

- (NSInteger)horizontalIndexForPoint:(CGPoint)point indexClamp:(JBLineChartHorizontalIndexClamp)indexClamp
{
    NSArray *largestLineData = nil;
    for (NSArray *lineData in self.chartData)
    {
        if ([lineData count] > [largestLineData count])
        {
            largestLineData = lineData;
        }
    }
    return [self horizontalIndexForPoint:point indexClamp:indexClamp lineData:largestLineData];
}

- (NSInteger)horizontalIndexForPoint:(CGPoint)point
{
    return [self horizontalIndexForPoint:point indexClamp:JBLineChartHorizontalIndexClampNone];
}

- (NSInteger)lineIndexForPoint:(CGPoint)point
{
    // Find the horizontal indexes
    NSUInteger leftHorizontalIndex = [self horizontalIndexForPoint:point indexClamp:JBLineChartHorizontalIndexClampLeft];
    NSUInteger rightHorizontalIndex = [self horizontalIndexForPoint:point indexClamp:JBLineChartHorizontalIndexClampRight];
    
    // Padding
    CGFloat chartPadding = [self padding];
    
    NSUInteger shortestDistance = INT_MAX;
    NSInteger selectedIndex = kJBLineChartUnselectedLineIndex;
    NSAssert([self.dataSource respondsToSelector:@selector(numberOfLinesInLineChartView:)], @"JBLineChartView // dataSource must implement - (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView");
    NSUInteger numberOfLines = [self.dataSource numberOfLinesInLineChartView:self];
    
    // Iterate all lines
    for (NSUInteger lineIndex=0; lineIndex<numberOfLines; lineIndex++)
    {
        NSAssert([self.dataSource respondsToSelector:@selector(lineChartView:numberOfVerticalValuesAtLineIndex:)], @"JBLineChartView // dataSource must implement - (NSUInteger)lineChartView:(JBLineChartView *)lineChartView numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex");
        if ([self.dataSource lineChartView:self numberOfVerticalValuesAtLineIndex:lineIndex] > rightHorizontalIndex)
        {
            NSArray *lineData = [self.chartData objectAtIndex:lineIndex];

            // Left point
            JBLineChartPoint *leftLineChartPoint = [lineData objectAtIndex:leftHorizontalIndex];
            CGPoint leftPoint = CGPointMake(leftLineChartPoint.position.x, fmin(fmax(chartPadding, self.linesView.bounds.size.height - leftLineChartPoint.position.y), self.linesView.bounds.size.height - chartPadding));
            
            // Right point
            JBLineChartPoint *rightLineChartPoint = [lineData objectAtIndex:rightHorizontalIndex];
            CGPoint rightPoint = CGPointMake(rightLineChartPoint.position.x, fmin(fmax(chartPadding, self.linesView.bounds.size.height - rightLineChartPoint.position.y), self.linesView.bounds.size.height - chartPadding));
            
            // Touch point
            CGPoint normalizedTouchPoint = CGPointMake(point.x, self.linesView.bounds.size.height - point.y);

            // Slope
            CGFloat lineSlope = (CGFloat)(rightPoint.y - leftPoint.y) / (CGFloat)(rightPoint.x - leftPoint.x);

            // Insersection point
            CGPoint interesectionPoint = CGPointMake(normalizedTouchPoint.x, (lineSlope * (normalizedTouchPoint.x - leftPoint.x)) + leftPoint.y);

            CGFloat currentDistance = fabs(interesectionPoint.y - normalizedTouchPoint.y);
            if (currentDistance < shortestDistance)
            {
                shortestDistance = currentDistance;
                selectedIndex = lineIndex;
            }
        }
    }
    return selectedIndex;
}

- (void)touchesBeganOrMovedWithTouches:(NSSet *)touches
{
    if (self.state == JBChartViewStateCollapsed || [self.chartData count] <= 0)
    {
        return; // no touch for no data or collapsed
    }
    
    UITouch *touch = [touches anyObject];
    CGPoint touchPoint = [self clampPoint:[touch locationInView:self.linesView] toBounds:self.linesView.bounds padding:[self padding]];
    
    NSUInteger lineIndex = self.linesView.selectedLineIndex != kJBLineChartLinesViewUnselectedLineIndex ? self.linesView.selectedLineIndex : [self lineIndexForPoint:touchPoint];
	
	if (lineIndex == kJBLineChartLinesViewUnselectedLineIndex || [[self.chartData objectAtIndex:lineIndex] count] <= 0)
	{
		return; // no touch for line without data
	}

    if ([self.delegate respondsToSelector:@selector(lineChartView:didSelectLineAtIndex:horizontalIndex:touchPoint:)])
    {
        NSUInteger horizontalIndex = [self horizontalIndexForPoint:touchPoint indexClamp:JBLineChartHorizontalIndexClampNone lineData:[self.chartData objectAtIndex:lineIndex]];
        [self.delegate lineChartView:self didSelectLineAtIndex:lineIndex horizontalIndex:horizontalIndex touchPoint:[touch locationInView:self]];
    }
    
    if ([self.delegate respondsToSelector:@selector(lineChartView:didSelectLineAtIndex:horizontalIndex:)])
    {
        [self.delegate lineChartView:self didSelectLineAtIndex:lineIndex horizontalIndex:[self horizontalIndexForPoint:touchPoint indexClamp:JBLineChartHorizontalIndexClampNone lineData:[self.chartData objectAtIndex:lineIndex]]];
    }
    
    if ([self.delegate respondsToSelector:@selector(lineChartView:verticalSelectionColorForLineAtLineIndex:)])
    {
        UIColor *verticalSelectionColor = [self.delegate lineChartView:self verticalSelectionColorForLineAtLineIndex:lineIndex];
        NSAssert(verticalSelectionColor != nil, @"JBLineChartView // delegate function - (UIColor *)lineChartView:(JBLineChartView *)lineChartView verticalSelectionColorForLineAtLineIndex:(NSUInteger)lineIndex must return a non-nil UIColor");
        self.verticalSelectionView.bgColor = verticalSelectionColor;
    }
    
    
    CGFloat xOffset = fmin(self.bounds.size.width - self.verticalSelectionView.frame.size.width, fmax(0, touchPoint.x - (ceil(self.verticalSelectionView.frame.size.width * 0.5))));
    CGFloat yOffset = self.headerView.frame.size.height + self.headerPadding;
    
    if ([self.dataSource respondsToSelector:@selector(shouldExtendSelectionViewIntoHeaderPaddingForChartView:)])
    {
        if ([self.dataSource shouldExtendSelectionViewIntoHeaderPaddingForChartView:self])
        {
            yOffset = self.headerView.frame.size.height;
        }
    }
    
    self.verticalSelectionView.frame = CGRectMake(xOffset, yOffset, self.verticalSelectionView.frame.size.width, self.verticalSelectionView.frame.size.height);
    [self setVerticalSelectionViewVisible:YES animated:YES];
}

- (void)touchesEndedOrCancelledWithTouches:(NSSet *)touches
{
    if (self.state == JBChartViewStateCollapsed || [self.chartData count] <= 0)
    {
        return;
    }

    [self setVerticalSelectionViewVisible:NO animated:YES];
    
    if ([self.delegate respondsToSelector:@selector(didDeselectLineInLineChartView:)])
    {
        [self.delegate didDeselectLineInLineChartView:self];
    }
    
    if (self.showsLineSelection)
    {
        [self.linesView setSelectedLineIndex:kJBLineChartLinesViewUnselectedLineIndex animated:YES];
        [self.dotsView setSelectedLineIndex:kJBLineChartDotsViewUnselectedLineIndex animated:YES];
    }
}

#pragma mark - Gestures

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch *touch = [touches anyObject];
    CGPoint touchPoint = [self clampPoint:[touch locationInView:self.linesView] toBounds:self.linesView.bounds padding:[self padding]];
    if (self.showsLineSelection)
    {
        [self.linesView setSelectedLineIndex:[self lineIndexForPoint:touchPoint] animated:YES];
        [self.dotsView setSelectedLineIndex:[self lineIndexForPoint:touchPoint] animated:YES];
    }
    [self touchesBeganOrMovedWithTouches:touches];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self touchesBeganOrMovedWithTouches:touches];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self touchesEndedOrCancelledWithTouches:touches];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self touchesEndedOrCancelledWithTouches:touches];
}

@end

@implementation JBLineLayer

#pragma mark - Alloc/Init

+ (void)initialize
{
	if (self == [JBLineLayer class])
	{
		kJBLineChartLineViewDefaultDashPattern = @[@(3), @(2)];
	}
}

- (id)init
{
    self = [super init];
    if (self)
    {
        self.zPosition = 0.1f;
        self.fillColor = [UIColor clearColor].CGColor;
    }
    return self;
}

#pragma mark - Setters

- (void)setLineStyle:(JBLineChartViewLineStyle)lineStyle
{
    _lineStyle = lineStyle;
    
    if (_lineStyle == JBLineChartViewLineStyleDashed)
    {
        self.lineDashPhase = kJBLineChartLinesViewDefaultLinePhase;
        self.lineDashPattern = kJBLineChartLineViewDefaultDashPattern;
    }
    else if (_lineStyle == JBLineChartViewLineStyleSolid)
    {
        self.lineDashPhase = 0.0;
        self.lineDashPattern = nil;
    }
}

@end

@implementation JBFillLayer

#pragma mark - Alloc/Init

- (id)init
{
    self = [super init];
    if (self)
    {
        self.zPosition = 0.0f;
        self.strokeColor = [UIColor clearColor].CGColor;
        self.lineWidth = 0.0f;
    }
    return self;
}

@end

@implementation JBLineChartPoint

#pragma mark - Alloc/Init

- (id)init
{
    self = [super init];
    if (self)
    {
        _position = CGPointZero;
    }
    return self;
}

#pragma mark - Compare

- (NSComparisonResult)compare:(JBLineChartPoint *)otherObject
{
    return self.position.x > otherObject.position.x;
}

@end

@implementation JBLineChartLinesView

#pragma mark - Alloc/Init

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self)
    {
        self.backgroundColor = [UIColor clearColor];
    }
    return self;
}

#pragma mark - Memory Management

- (void)dealloc
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self];
}

#pragma mark - Drawing

- (void)drawRect:(CGRect)rect
{
    [super drawRect:rect];

    NSAssert([self.delegate respondsToSelector:@selector(chartDataForLineChartLinesView:)], @"JBLineChartLinesView // delegate must implement - (NSArray *)chartDataForLineChartLinesView:(JBLineChartLinesView *)lineChartLinesView");
    NSArray *chartData = [self.delegate chartDataForLineChartLinesView:self];

    NSAssert([self.delegate respondsToSelector:@selector(paddingForLineChartLinesView:)], @"JBLineChartLinesView // delegate must implement - (CGFloat)paddingForLineChartLinesView:(JBLineChartLinesView *)lineChartLinesView");
    CGFloat padding = [self.delegate paddingForLineChartLinesView:self];
    
    NSUInteger lineIndex = 0;
    for (NSArray *lineData in chartData)
    {
        if (lineData.count == 0)
        {
            continue;
        }

        UIBezierPath *path = [UIBezierPath bezierPath];
        path.miterLimit = kJBLineChartLinesViewMiterLimit;
        
        JBLineChartPoint *previousLineChartPoint = nil;
        CGFloat previousSlope = 0.0f;
        
        NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:smoothLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (BOOL)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView smoothLineAtLineIndex:(NSUInteger)lineIndex");
        BOOL smoothLine = [self.delegate lineChartLinesView:self smoothLineAtLineIndex:lineIndex];
        
        BOOL visiblePointFound = NO;
        NSArray *sortedLineData = [lineData sortedArrayUsingSelector:@selector(compare:)];
        CGFloat firstXPosition = 0.0f;
        CGFloat lastXPosition = 0.0f;
        for (NSUInteger index = 0; index < [sortedLineData count]; index++)
        {
            JBLineChartPoint *lineChartPoint = [sortedLineData objectAtIndex:index];
            if(lineChartPoint.hidden) {
                continue;
            }

            if (!visiblePointFound)
            {
                [path moveToPoint:CGPointMake(lineChartPoint.position.x, fmin(self.bounds.size.height - padding, fmax(padding, lineChartPoint.position.y)))];
                firstXPosition = lineChartPoint.position.x;
                visiblePointFound = YES;
            }
            else
            {
                JBLineChartPoint *nextLineChartPoint = nil;
                if (index != ([lineData count] - 1))
                {
                    nextLineChartPoint = [sortedLineData objectAtIndex:(index + 1)];
                }
                
                CGFloat nextSlope = (nextLineChartPoint != nil) ? ((nextLineChartPoint.position.y - lineChartPoint.position.y)) / ((nextLineChartPoint.position.x - lineChartPoint.position.x)) : previousSlope;
                CGFloat currentSlope = ((lineChartPoint.position.y - previousLineChartPoint.position.y)) / (lineChartPoint.position.x-previousLineChartPoint.position.x);
                
                BOOL deltaFromNextSlope = ((currentSlope >= (nextSlope + kJBLineChartLinesViewSmoothThresholdSlope)) || (currentSlope <= (nextSlope - kJBLineChartLinesViewSmoothThresholdSlope)));
                BOOL deltaFromPreviousSlope = ((currentSlope >= (previousSlope + kJBLineChartLinesViewSmoothThresholdSlope)) || (currentSlope <= (previousSlope - kJBLineChartLinesViewSmoothThresholdSlope)));
                BOOL deltaFromPreviousY = (lineChartPoint.position.y >= previousLineChartPoint.position.y + kJBLineChartLinesViewSmoothThresholdVertical) || (lineChartPoint.position.y <= previousLineChartPoint.position.y - kJBLineChartLinesViewSmoothThresholdVertical);
                
                if (smoothLine && deltaFromNextSlope && deltaFromPreviousSlope && deltaFromPreviousY)
                {
                    CGFloat deltaX = lineChartPoint.position.x - previousLineChartPoint.position.x;
                    CGFloat controlPointX = previousLineChartPoint.position.x + (deltaX / 2);
                    
                    CGPoint controlPoint1 = CGPointMake(controlPointX, previousLineChartPoint.position.y);
                    CGPoint controlPoint2 = CGPointMake(controlPointX, lineChartPoint.position.y);
                    
                    [path addCurveToPoint:CGPointMake(lineChartPoint.position.x, fmin(self.bounds.size.height - padding, fmax(padding, lineChartPoint.position.y))) controlPoint1:controlPoint1 controlPoint2:controlPoint2];
                }
                else
                {
                    [path addLineToPoint:CGPointMake(lineChartPoint.position.x, fmin(self.bounds.size.height - padding, fmax(padding, lineChartPoint.position.y)))];
                }
                
                lastXPosition = lineChartPoint.position.x;
                previousSlope = currentSlope;
            }
            previousLineChartPoint = lineChartPoint;
        }
        
        JBLineLayer *shapeLayer = [self lineLayerForLineIndex:lineIndex];
        if (shapeLayer == nil)
        {
            shapeLayer = [JBLineLayer layer];
        }
        
        JBFillLayer *shapeFillLayer = [self fillLayerForLineIndex:lineIndex];
        if (shapeFillLayer == nil)
        {
            shapeFillLayer = [JBFillLayer layer];
        }
        
        shapeLayer.tag = lineIndex;
        shapeFillLayer.tag = lineIndex;
        
        NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:lineStyleForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (JBLineChartViewLineStyle)lineChartLineView:(JBLineChartLinesView *)lineChartLinesView lineStyleForLineAtLineIndex:(NSUInteger)lineIndex");
        shapeLayer.lineStyle = [self.delegate lineChartLinesView:self lineStyleForLineAtLineIndex:lineIndex];
        
        NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:colorForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView colorForLineAtLineIndex:(NSUInteger)lineIndex");
        shapeLayer.strokeColor = [self.delegate lineChartLinesView:self colorForLineAtLineIndex:lineIndex].CGColor;
        
        NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:fillColorForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView fillColorForLineAtLineIndex:(NSUInteger)lineIndex");
        shapeFillLayer.fillColor = [self.delegate lineChartLinesView:self fillColorForLineAtLineIndex:lineIndex].CGColor;
        
        if (smoothLine == YES)
        {
            shapeLayer.lineCap = kCALineCapRound;
            shapeLayer.lineJoin = kCALineJoinRound;
            shapeFillLayer.lineCap = kCALineCapRound;
            shapeFillLayer.lineJoin = kCALineJoinRound;
        }
        else
        {
            shapeLayer.lineCap = kCALineCapButt;
            shapeLayer.lineJoin = kCALineJoinMiter;
            shapeFillLayer.lineCap = kCALineCapButt;
            shapeFillLayer.lineJoin = kCALineJoinMiter;
        }
        
        NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:widthForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (CGFloat)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView widthForLineAtLineIndex:(NSUInteger)lineIndex");
        shapeLayer.lineWidth = [self.delegate lineChartLinesView:self widthForLineAtLineIndex:lineIndex];
        shapeLayer.path = path.CGPath;
        shapeLayer.frame = self.bounds;
        
        // Continue the path for the fill layer (close and fill)
        UIBezierPath *fillPath = [path copy];
        
        if(visiblePointFound)
        {
            [fillPath addLineToPoint:CGPointMake(lastXPosition, self.bounds.size.height - padding)];
            [fillPath addLineToPoint:CGPointMake(firstXPosition, self.bounds.size.height - padding)];
        }

        shapeFillLayer.path = fillPath.CGPath;
        shapeFillLayer.frame = self.bounds;
        
        [self.layer addSublayer:shapeFillLayer];
        [self.layer addSublayer:shapeLayer];

        lineIndex++;
    }

    self.animated = NO;
}

#pragma mark - Data

- (void)reloadData
{
    // Drawing is all done with CG (no subviews here)
    [self setNeedsDisplay];
}

#pragma mark - Setters

- (void)setSelectedLineIndex:(NSInteger)selectedLineIndex animated:(BOOL)animated
{
    _selectedLineIndex = selectedLineIndex;
    
    __weak JBLineChartLinesView* weakSelf = self;
    
    dispatch_block_t adjustLines = ^{
        for (CALayer *layer in [weakSelf.layer sublayers])
        {
            if ([layer isKindOfClass:[JBLineLayer class]])
            {
                if (((NSInteger)((JBLineLayer *)layer).tag) == weakSelf.selectedLineIndex)
                {
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:selectedColorForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView selectedColorForLineAtLineIndex:(NSUInteger)lineIndex");
                    ((JBLineLayer *)layer).strokeColor = [self.delegate lineChartLinesView:self selectedColorForLineAtLineIndex:((JBLineLayer *)layer).tag].CGColor;
                    ((JBLineLayer *)layer).opacity = 1.0f;
                }
                else
                {
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:colorForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView colorForLineAtLineIndex:(NSUInteger)lineIndex");
                    ((JBLineLayer *)layer).strokeColor = [self.delegate lineChartLinesView:self colorForLineAtLineIndex:((JBLineLayer *)layer).tag].CGColor;
                    ((JBLineLayer *)layer).opacity = (weakSelf.selectedLineIndex == kJBLineChartLinesViewUnselectedLineIndex) ? 1.0f : kJBLineChartLinesViewDefaultDimmedOpacity;
                }
            }
            else if ([layer isKindOfClass:[JBFillLayer class]])
            {
                if (((NSInteger)((JBFillLayer *)layer).tag) == weakSelf.selectedLineIndex)
                {
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:selectedFillColorForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView selectedFillColorForLineAtLineIndex:(NSUInteger)lineIndex");
                    ((JBFillLayer *)layer).fillColor = [self.delegate lineChartLinesView:self selectedFillColorForLineAtLineIndex:((JBLineLayer *)layer).tag].CGColor;
                    ((JBFillLayer *)layer).opacity = 1.0f;
                }
                else
                {
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartLinesView:fillColorForLineAtLineIndex:)], @"JBLineChartLinesView // delegate must implement - (UIColor *)lineChartLinesView:(JBLineChartLinesView *)lineChartLinesView fillColorForLineAtLineIndex:(NSUInteger)lineIndex");
                    ((JBFillLayer *)layer).fillColor = [self.delegate lineChartLinesView:self fillColorForLineAtLineIndex:((JBLineLayer *)layer).tag].CGColor;
                    ((JBFillLayer *)layer).opacity = (weakSelf.selectedLineIndex == kJBLineChartLinesViewUnselectedLineIndex) ? 1.0f : kJBLineChartLinesViewDefaultDimmedOpacity;
                }
            }
        }
    };

    if (animated)
    {
        [UIView animateWithDuration:kJBChartViewDefaultAnimationDuration animations:^{
            adjustLines();
        }];
    }
    else
    {
        adjustLines();
    }
}

- (void)setSelectedLineIndex:(NSInteger)selectedLineIndex
{
    [self setSelectedLineIndex:selectedLineIndex animated:NO];
}

#pragma mark - Callback Helpers

- (void)fireCallback:(void (^)())callback
{
    dispatch_block_t callbackCopy = [callback copy];

    if (callbackCopy != nil)
    {
        callbackCopy();
    }
}

#pragma mark - View Helpers

- (JBLineLayer *)lineLayerForLineIndex:(NSUInteger)lineIndex
{
    for (CALayer *layer in [self.layer sublayers])
    {
        if ([layer isKindOfClass:[JBLineLayer class]])
        {
            if (((JBLineLayer *)layer).tag == lineIndex)
            {
                return (JBLineLayer *)layer;
            }
        }
    }
    return nil;
}

- (JBFillLayer *)fillLayerForLineIndex:(NSUInteger)lineIndex
{
    for (CALayer *layer in [self.layer sublayers])
    {
        if ([layer isKindOfClass:[JBFillLayer class]])
        {
            if (((JBFillLayer *)layer).tag == lineIndex)
            {
                return (JBFillLayer *)layer;
            }
        }
    }
    return nil;
}

@end

@implementation JBLineChartDotsView

#pragma mark - Alloc/Init

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self)
    {
        self.backgroundColor = [UIColor clearColor];
    }
    return self;
}

#pragma mark - Data

- (void)reloadData
{
    for (NSArray *dotViews in [self.dotViewsDict allValues])
    {
        for (JBLineChartDotView *dotView in dotViews)
        {
            [dotView removeFromSuperview];
        }
    }
    
    NSAssert([self.delegate respondsToSelector:@selector(chartDataForLineChartDotsView:)], @"JBLineChartDotsView // delegate must implement - (NSArray *)chartDataForLineChartDotsView:(JBLineChartDotsView *)lineChartDotsView");
    NSArray *chartData = [self.delegate chartDataForLineChartDotsView:self];

    NSAssert([self.delegate respondsToSelector:@selector(paddingForLineChartDotsView:)], @"JBLineChartDotsView // delegate must implement - (CGFloat)paddingForLineChartDotsView:(JBLineChartDotsView *)lineChartDotsView");
    CGFloat padding = [self.delegate paddingForLineChartDotsView:self];
    
    NSUInteger lineIndex = 0;
    NSMutableDictionary *mutableDotViewsDict = [NSMutableDictionary dictionary];
    for (NSArray *lineData in chartData)
    {
        NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:showsDotsForLineAtLineIndex:)], @"JBLineChartDotsView // delegate must implement - (BOOL)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView showsDotsForLineAtLineIndex:(NSUInteger)lineIndex");
        
        if ([self.delegate lineChartDotsView:self showsDotsForLineAtLineIndex:lineIndex]) // line at index contains dots
        {
            NSMutableArray *mutableDotViews = [NSMutableArray array];
            NSArray *sortedLineData = [lineData sortedArrayUsingSelector:@selector(compare:)];
            for (NSUInteger horizontalIndex = 0; horizontalIndex < [sortedLineData count]; horizontalIndex++)
            {
                JBLineChartPoint *lineChartPoint = [sortedLineData objectAtIndex:horizontalIndex];
                if(lineChartPoint.hidden)
                {
                    continue;
                }

                NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:dotViewAtHorizontalIndex:atLineIndex:)], @"JBLineChartDotsView // delegate must implement - (UIView *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView dotViewAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                UIView *currentDotView = [self.delegate lineChartDotsView:self dotViewAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                
                // System dot
                if (currentDotView == nil)
                {
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:dotRadiusForLineAtHorizontalIndex:atLineIndex:)], @"JBLineChartDotsView // delegate must implement - (CGFloat)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView dotRadiusForLineAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                    CGFloat dotRadius = [self.delegate lineChartDotsView:self dotRadiusForLineAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                    
                    currentDotView = [[JBLineChartDotView alloc] initWithRadius:dotRadius];
                    
                    NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:colorForDotAtHorizontalIndex:atLineIndex:)], @"JBLineChartDotsView // delegate must implement - (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView colorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                    currentDotView.backgroundColor = [self.delegate lineChartDotsView:self colorForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                }

                currentDotView.center = CGPointMake(lineChartPoint.position.x, fmin(self.bounds.size.height - padding, fmax(padding, lineChartPoint.position.y)));
                [mutableDotViews addObject:currentDotView];
                [self addSubview:currentDotView];

            }
            [mutableDotViewsDict setObject:[NSArray arrayWithArray:mutableDotViews] forKey:[NSNumber numberWithInteger:lineIndex]];
        }
        lineIndex++;
    }
    self.dotViewsDict = [NSDictionary dictionaryWithDictionary:mutableDotViewsDict];
}

#pragma mark - Setters

- (void)setSelectedLineIndex:(NSInteger)selectedLineIndex animated:(BOOL)animated
{
    _selectedLineIndex = selectedLineIndex;
    
    __weak JBLineChartDotsView* weakSelf = self;
    
    dispatch_block_t adjustDots = ^{
        [weakSelf.dotViewsDict enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
            NSUInteger horizontalIndex = 0;
            for (UIView *dotView in (NSArray *)obj)
            {
                if ([key isKindOfClass:[NSNumber class]])
                {
                    NSInteger lineIndex = [((NSNumber *)key) intValue];

                    // Internal dot
                    if ([dotView isKindOfClass:[JBLineChartDotView class]])
                    {
                        if (weakSelf.selectedLineIndex == lineIndex)
                        {
                            NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:selectedColorForDotAtHorizontalIndex:atLineIndex:)], @"JBLineChartDotsView // delegate must implement - (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView selectedColorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                            dotView.backgroundColor = [self.delegate lineChartDotsView:self selectedColorForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                        }
                        else
                        {
                            NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:colorForDotAtHorizontalIndex:atLineIndex:)], @"JBLineChartDotsView // delegate must implement - (UIColor *)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView colorForDotAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                            dotView.backgroundColor = [self.delegate lineChartDotsView:self colorForDotAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                            dotView.alpha = (weakSelf.selectedLineIndex == kJBLineChartDotsViewUnselectedLineIndex) ? 1.0f : 0.0f; // hide dots on off-selection
                        }
                    }
                    // Custom dot
                    else
                    {
                        NSAssert([self.delegate respondsToSelector:@selector(lineChartDotsView:shouldHideDotViewOnSelectionAtHorizontalIndex:atLineIndex:)], @"JBLineChartDotsView // delegate must implement - (BOOL)lineChartDotsView:(JBLineChartDotsView *)lineChartDotsView shouldHideDotViewOnSelectionAtHorizontalIndex:(NSUInteger)horizontalIndex atLineIndex:(NSUInteger)lineIndex");
                        BOOL hideDotView = [self.delegate lineChartDotsView:self shouldHideDotViewOnSelectionAtHorizontalIndex:horizontalIndex atLineIndex:lineIndex];
                        if (weakSelf.selectedLineIndex == lineIndex)
                        {
                            dotView.alpha = hideDotView ? 0.0f : 1.0f;
                        }
                        else
                        {
                            dotView.alpha = 1.0;
                        }
                    }
                }
                horizontalIndex++;
            }
        }];
    };
    
    if (animated)
    {
        [UIView animateWithDuration:kJBChartViewDefaultAnimationDuration animations:^{
            adjustDots();
        }];
    }
    else
    {
        adjustDots();
    }
}

- (void)setSelectedLineIndex:(NSInteger)selectedLineIndex
{
    [self setSelectedLineIndex:selectedLineIndex animated:NO];
}

@end

@implementation JBLineChartDotView

#pragma mark - Alloc/Init

- (id)initWithRadius:(CGFloat)radius
{
    self = [super initWithFrame:CGRectMake(0, 0, (radius * 2.0f), (radius * 2.0f))];
    if (self)
    {
        self.clipsToBounds = YES;
        self.layer.cornerRadius = ((radius * 2.0f) * 0.5f);
    }
    return self;
}

@end
