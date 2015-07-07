//
//  JBBarChartView.m
//  Nudge
//
//  Created by Terry Worona on 9/3/13.
//  Copyright (c) 2013 Jawbone. All rights reserved.
//

#import "JBBarChartView.h"

// Numerics
CGFloat static const kJBBarChartViewBarBasePaddingMutliplier = 50.0f;
CGFloat static const kJBBarChartViewUndefinedCachedHeight = -1.0f;
CGFloat static const kJBBarChartViewStateAnimationDuration = 0.05f;
CGFloat static const kJBBarChartViewStatePopOffset = 10.0f;
NSInteger static const kJBBarChartViewUndefinedBarIndex = -1;

// Colors (JBChartView)
static UIColor *kJBBarChartViewDefaultBarColor = nil;

@interface JBChartView (Private)

- (BOOL)hasMaximumValue;
- (BOOL)hasMinimumValue;

@end

@interface JBBarChartView ()

@property (nonatomic, strong) NSDictionary *chartDataDictionary; // key = column, value = height
@property (nonatomic, strong) NSArray *barViews;
@property (nonatomic, strong) NSArray *cachedBarViewHeights;
@property (nonatomic, assign) CGFloat barPadding;
@property (nonatomic, assign) CGFloat cachedMaxHeight;
@property (nonatomic, assign) CGFloat cachedMinHeight;
@property (nonatomic, strong) JBChartVerticalSelectionView *verticalSelectionView;
@property (nonatomic, assign) BOOL verticalSelectionViewVisible;

// Initialization
- (void)construct;

// View quick accessors
- (CGFloat)availableHeight;
- (CGFloat)normalizedHeightForRawHeight:(NSNumber*)rawHeight;
- (CGFloat)barWidth;

// Touch helpers
- (NSInteger)barViewIndexForPoint:(CGPoint)point;
- (UIView *)barViewForForPoint:(CGPoint)point;
- (void)touchesBeganOrMovedWithTouches:(NSSet *)touches;
- (void)touchesEndedOrCancelledWithTouches:(NSSet *)touches;

// Setters
- (void)setVerticalSelectionViewVisible:(BOOL)verticalSelectionViewVisible animated:(BOOL)animated;

@end

@implementation JBBarChartView

@dynamic dataSource;
@dynamic delegate;

#pragma mark - Alloc/Init

+ (void)initialize
{
	if (self == [JBBarChartView class])
	{
		kJBBarChartViewDefaultBarColor = [UIColor blackColor];
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
    _cachedMinHeight = kJBBarChartViewUndefinedCachedHeight;
    _cachedMaxHeight = kJBBarChartViewUndefinedCachedHeight;
}

#pragma mark - Memory Management

- (void)dealloc
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self];
}

#pragma mark - Data

- (void)reloadData
{
    // reset cached max height
    self.cachedMinHeight = kJBBarChartViewUndefinedCachedHeight;
    self.cachedMaxHeight = kJBBarChartViewUndefinedCachedHeight;
    
    /*
     * The data collection holds all position information:
     * constructed via datasource and delegate functions
     */
    dispatch_block_t createDataDictionaries = ^{
        
        // Grab the count
        NSAssert([self.dataSource respondsToSelector:@selector(numberOfBarsInBarChartView:)], @"JBBarChartView // datasource must implement - (NSUInteger)numberOfBarsInBarChartView:(JBBarChartView *)barChartView");
        NSUInteger dataCount = [self.dataSource numberOfBarsInBarChartView:self];

        // Build up the data collection
        NSAssert([self.delegate respondsToSelector:@selector(barChartView:heightForBarViewAtIndex:)], @"JBBarChartView // delegate must implement - (CGFloat)barChartView:(JBBarChartView *)barChartView heightForBarViewAtIndex:(NSUInteger)index");
        NSMutableDictionary *dataDictionary = [NSMutableDictionary dictionary];
        for (NSUInteger index=0; index<dataCount; index++)
        {
            CGFloat height = [self.delegate barChartView:self heightForBarViewAtIndex:index];
            NSAssert(height >= 0, @"JBBarChartView // datasource function - (CGFloat)barChartView:(JBBarChartView *)barChartView heightForBarViewAtIndex:(NSUInteger)index must return a CGFloat >= 0");
            [dataDictionary setObject:[NSNumber numberWithFloat:height] forKey:[NSNumber numberWithInt:(int)index]];
        }
        self.chartDataDictionary = [NSDictionary dictionaryWithDictionary:dataDictionary];
	};
    
    /*
     * Determines the padding between bars as a function of # of bars
     */
    dispatch_block_t createBarPadding = ^{
        if ([self.delegate respondsToSelector:@selector(barPaddingForBarChartView:)])
        {
            self.barPadding = [self.delegate barPaddingForBarChartView:self];
        }
        else
        {
            NSUInteger totalBars = [[self.chartDataDictionary allKeys] count];
            self.barPadding = (1/(float)totalBars) * kJBBarChartViewBarBasePaddingMutliplier;
        }
    };
    
    /*
     * Creates a new bar graph view using the previously calculated data model
     */
    dispatch_block_t createBars = ^{
        
        // Remove old bars
        for (UIView *barView in self.barViews)
        {
            [barView removeFromSuperview];
        }
        
        self.cachedBarViewHeights = nil;
        
        CGFloat xOffset = 0;
        NSUInteger index = 0;
        NSMutableArray *mutableBarViews = [NSMutableArray array];
        NSMutableArray *mutableCachedBarViewHeights = [NSMutableArray array];
        for (NSNumber *key in [[self.chartDataDictionary allKeys] sortedArrayUsingSelector:@selector(compare:)])
        {
            UIView *barView = nil; // since all bars are visible at once, no need to cache this view
            if ([self.dataSource respondsToSelector:@selector(barChartView:barViewAtIndex:)])
            {
                barView = [self.dataSource barChartView:self barViewAtIndex:index];
                NSAssert(barView != nil, @"JBBarChartView // datasource function - (UIView *)barChartView:(JBBarChartView *)barChartView barViewAtIndex:(NSUInteger)index must return a non-nil UIView subclass");
            }
            else
            {
                barView = [[UIView alloc] init];
                UIColor *backgroundColor = nil;

                if ([self.delegate respondsToSelector:@selector(barChartView:colorForBarViewAtIndex:)])
                {
                    backgroundColor = [self.delegate barChartView:self colorForBarViewAtIndex:index];
                    NSAssert(backgroundColor != nil, @"JBBarChartView // delegate function - (UIColor *)barChartView:(JBBarChartView *)barChartView colorForBarViewAtIndex:(NSUInteger)index must return a non-nil UIColor");
                }
                else
                {
                    backgroundColor = kJBBarChartViewDefaultBarColor;
                }

                barView.backgroundColor = backgroundColor;
            }
            
            barView.tag = index;

            CGFloat height = [self normalizedHeightForRawHeight:[self.chartDataDictionary objectForKey:key]];
            barView.frame = CGRectMake(xOffset, self.bounds.size.height - height - self.footerView.frame.size.height, [self barWidth], height);
            [mutableBarViews addObject:barView];
            [mutableCachedBarViewHeights addObject:[NSNumber numberWithFloat:height]];
			
            // Add new bar
            if (self.footerView)
			{
				[self insertSubview:barView belowSubview:self.footerView];
			}
			else
			{
				[self addSubview:barView];
			}
            
            xOffset += ([self barWidth] + self.barPadding);
            index++;
        }
        self.barViews = [NSArray arrayWithArray:mutableBarViews];
        self.cachedBarViewHeights = [NSArray arrayWithArray:mutableCachedBarViewHeights];
    };
    
    /*
     * Creates a vertical selection view for touch events
     */
    dispatch_block_t createSelectionView = ^{
        
        // Remove old selection bar
        if (self.verticalSelectionView)
        {
            [self.verticalSelectionView removeFromSuperview];
            self.verticalSelectionView = nil;
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

        self.verticalSelectionView = [[JBChartVerticalSelectionView alloc] initWithFrame:CGRectMake(0, 0, [self barWidth], verticalSelectionViewHeight)];
        self.verticalSelectionView.alpha = 0.0;
        self.verticalSelectionView.hidden = !self.showsVerticalSelection;
        if ([self.delegate respondsToSelector:@selector(barSelectionColorForBarChartView:)])
        {
            UIColor *selectionViewBackgroundColor = [self.delegate barSelectionColorForBarChartView:self];
            NSAssert(selectionViewBackgroundColor != nil, @"JBBarChartView // delegate function - (UIColor *)barSelectionColorForBarChartView:(JBBarChartView *)barChartView must return a non-nil UIColor");
            self.verticalSelectionView.bgColor = selectionViewBackgroundColor;
        }
        
        // Add new selection bar
        if (self.footerView)
        {
            [self insertSubview:self.verticalSelectionView belowSubview:self.footerView];
        }
        else
        {
            [self addSubview:self.verticalSelectionView];
        }
        
        self.verticalSelectionView.transform = self.inverted ? CGAffineTransformMakeScale(1.0, -1.0) : CGAffineTransformIdentity;
    };
    
    createDataDictionaries();
    createBarPadding();
    createBars();
    createSelectionView();
    
    // Position header and footer
    self.headerView.frame = CGRectMake(self.bounds.origin.x, self.bounds.origin.y, self.bounds.size.width, self.headerView.frame.size.height);
    self.footerView.frame = CGRectMake(self.bounds.origin.x, self.bounds.size.height - self.footerView.frame.size.height, self.bounds.size.width, self.footerView.frame.size.height);

    // Refresh state
    [self setState:self.state animated:NO force:YES callback:nil];
}

#pragma mark - View Quick Accessors

- (CGFloat)availableHeight
{
    return self.bounds.size.height - self.headerView.frame.size.height - self.footerView.frame.size.height - self.headerPadding - self.footerPadding;
}

- (CGFloat)normalizedHeightForRawHeight:(NSNumber*)rawHeight
{
    CGFloat minHeight = [self minimumValue];
    CGFloat maxHeight = [self maximumValue];
    CGFloat value = [rawHeight floatValue];
    
    if ((maxHeight - minHeight) <= 0)
    {
        return 0;
    }
    
    return ((value - minHeight) / (maxHeight - minHeight)) * [self availableHeight];
}

- (CGFloat)barWidth
{
    NSUInteger barCount = [[self.chartDataDictionary allKeys] count];
    if (barCount > 0)
    {
        CGFloat totalPadding = (barCount - 1) * self.barPadding;
        CGFloat availableWidth = self.bounds.size.width - totalPadding;
        return availableWidth / barCount;
    }
    return 0;
}

#pragma mark - Setters

- (void)setState:(JBChartViewState)state animated:(BOOL)animated force:(BOOL)force callback:(void (^)())callback
{
    [super setState:state animated:animated force:force callback:callback];
    
    __weak JBBarChartView* weakSelf = self;
    
    void (^updateBarView)(UIView *barView, BOOL popBar);
    
    updateBarView = ^(UIView *barView, BOOL popBar) {
        if (weakSelf.inverted)
        {
            if (weakSelf.state == JBChartViewStateExpanded)
            {
                if (popBar)
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.headerView.frame.size.height + weakSelf.headerPadding, barView.frame.size.width, [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue] + kJBBarChartViewStatePopOffset);
                }
                else
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.headerView.frame.size.height + weakSelf.headerPadding, barView.frame.size.width, [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue]);
                }
            }
            else if (weakSelf.state == JBChartViewStateCollapsed)
            {
                if (popBar)
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.headerView.frame.size.height + weakSelf.headerPadding, barView.frame.size.width, [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue] + kJBBarChartViewStatePopOffset);
                }
                else
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.headerView.frame.size.height + weakSelf.headerPadding, barView.frame.size.width, 0.0f);
                }
            }
        }
        else
        {
            if (weakSelf.state == JBChartViewStateExpanded)
            {
                if (popBar)
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.bounds.size.height - weakSelf.footerView.frame.size.height - weakSelf.footerPadding - [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue] - kJBBarChartViewStatePopOffset, barView.frame.size.width, [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue] + kJBBarChartViewStatePopOffset);
                }
                else
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.bounds.size.height - weakSelf.footerView.frame.size.height - weakSelf.footerPadding - [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue], barView.frame.size.width, [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue]);
                }
            }
            else if (weakSelf.state == JBChartViewStateCollapsed)
            {
                if (popBar)
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.bounds.size.height - weakSelf.footerView.frame.size.height - weakSelf.footerPadding - [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue] - kJBBarChartViewStatePopOffset, barView.frame.size.width, [[weakSelf.cachedBarViewHeights objectAtIndex:barView.tag] floatValue] + kJBBarChartViewStatePopOffset);
                }
                else
                {
                    barView.frame = CGRectMake(barView.frame.origin.x, weakSelf.bounds.size.height, barView.frame.size.width, 0.0f);
                }
            }
        }
    };
    
    dispatch_block_t callbackCopy = [callback copy];
    
    if ([self.barViews count] > 0)
    {
        if (animated)
        {
            NSUInteger index = 0;
            for (UIView *barView in self.barViews)
            {
                [UIView animateWithDuration:kJBBarChartViewStateAnimationDuration delay:(kJBBarChartViewStateAnimationDuration * 0.5) * index options:UIViewAnimationOptionBeginFromCurrentState animations:^{
                    updateBarView(barView, YES);
                } completion:^(BOOL finished) {
                    [UIView animateWithDuration:kJBBarChartViewStateAnimationDuration delay:0.0 options:UIViewAnimationOptionBeginFromCurrentState animations:^{
                        updateBarView(barView, NO);
                    } completion:^(BOOL lastBarFinished) {
                        if ((NSUInteger)barView.tag == [self.barViews count] - 1)
                        {
                            if (callbackCopy)
                            {
                                callbackCopy();
                            }
                        }
                    }];
                }];
                index++;
            }
        }
        else
        {
            for (UIView *barView in self.barViews)
            {
                updateBarView(barView, NO);
            }
            if (callbackCopy)
            {
                callbackCopy();
            }
        }
    }
    else
    {
        if (callbackCopy)
        {
            callbackCopy();
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
    if(_cachedMinHeight == kJBBarChartViewUndefinedCachedHeight)
    {
        NSArray *chartValues = [[NSMutableArray arrayWithArray:[self.chartDataDictionary allValues]] sortedArrayUsingSelector:@selector(compare:)];
        _cachedMinHeight =  [[chartValues firstObject] floatValue];
    }
    return _cachedMinHeight;
}

- (CGFloat)cachedMaxHeight
{
    if (_cachedMaxHeight == kJBBarChartViewUndefinedCachedHeight)
    {
        NSArray *chartValues = [[NSMutableArray arrayWithArray:[self.chartDataDictionary allValues]] sortedArrayUsingSelector:@selector(compare:)];
        _cachedMaxHeight =  [[chartValues lastObject] floatValue];
    }
    return _cachedMaxHeight;
}

- (CGFloat)minimumValue
{
    if ([self hasMinimumValue])
    {
        return fminf(self.cachedMinHeight, [super minimumValue]);
    }
    return self.cachedMinHeight;
}

- (CGFloat)maximumValue
{
    if ([self hasMaximumValue])
    {
        return fmaxf(self.cachedMaxHeight, [super maximumValue]);
    }
    return self.cachedMaxHeight;    
}

- (UIView *)barViewAtIndex:(NSUInteger)index
{
	if (index < [self.barViews count])
	{
		return [self.barViews objectAtIndex:index];
	}
	return nil;
}

#pragma mark - Touch Helpers

- (NSInteger)barViewIndexForPoint:(CGPoint)point
{
    NSUInteger index = 0;
    NSUInteger selectedIndex = kJBBarChartViewUndefinedBarIndex;
    
    if (point.x < 0 || point.x > self.bounds.size.width)
    {
        return selectedIndex;
    }
    
    CGFloat padding = ceil(self.barPadding * 0.5);
    for (UIView *barView in self.barViews)
    {
        CGFloat minX = CGRectGetMinX(barView.frame) - padding;
        CGFloat maxX = CGRectGetMaxX(barView.frame) + padding;
        if ((point.x >= minX) && (point.x <= maxX))
        {
            selectedIndex = index;
            break;
        }
        index++;
    }
    return selectedIndex;
}

- (UIView *)barViewForForPoint:(CGPoint)point
{
    UIView *barView = nil;
    NSInteger selectedIndex = [self barViewIndexForPoint:point];
    if (selectedIndex >= 0)
    {
        return [self.barViews objectAtIndex:[self barViewIndexForPoint:point]];
    }
    return barView;
}

- (void)touchesBeganOrMovedWithTouches:(NSSet *)touches
{
    if (self.state == JBChartViewStateCollapsed || [[self.chartDataDictionary allKeys] count] <= 0)
    {
        return;
    }
    
    UITouch *touch = [touches anyObject];
    CGPoint touchPoint = [touch locationInView:self];
    UIView *barView = [self barViewForForPoint:touchPoint];
    if (barView == nil)
    {
        [self setVerticalSelectionViewVisible:NO animated:YES];
        return;
    }
    CGRect barViewFrame = barView.frame;
    CGRect selectionViewFrame = self.verticalSelectionView.frame;
    selectionViewFrame.origin.x = barViewFrame.origin.x;
    selectionViewFrame.size.width = barViewFrame.size.width;
    
    if ([self.dataSource respondsToSelector:@selector(shouldExtendSelectionViewIntoHeaderPaddingForChartView:)])
    {
        if ([self.dataSource shouldExtendSelectionViewIntoHeaderPaddingForChartView:self])
        {
            selectionViewFrame.origin.y = self.headerView.frame.size.height;
        }
        else
        {
            selectionViewFrame.origin.y = self.headerView.frame.size.height + self.headerPadding;
        }
    }
    else
    {
        selectionViewFrame.origin.y = self.headerView.frame.size.height + self.headerPadding;
    }
    
    self.verticalSelectionView.frame = selectionViewFrame;
    [self setVerticalSelectionViewVisible:YES animated:YES];
    
    if ([self.delegate respondsToSelector:@selector(barChartView:didSelectBarAtIndex:touchPoint:)])
    {
        [self.delegate barChartView:self didSelectBarAtIndex:[self barViewIndexForPoint:touchPoint] touchPoint:touchPoint];
    }
    
    if ([self.delegate respondsToSelector:@selector(barChartView:didSelectBarAtIndex:)])
    {
        [self.delegate barChartView:self didSelectBarAtIndex:[self barViewIndexForPoint:touchPoint]];
    }
}

- (void)touchesEndedOrCancelledWithTouches:(NSSet *)touches
{
    if (self.state == JBChartViewStateCollapsed || [[self.chartDataDictionary allKeys] count] <= 0)
    {
        return;
    }
    
    [self setVerticalSelectionViewVisible:NO animated:YES];
    
    if ([self.delegate respondsToSelector:@selector(didDeselectBarChartView:)])
    {
        [self.delegate didDeselectBarChartView:self];
    }
}

#pragma mark - Touches

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
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
