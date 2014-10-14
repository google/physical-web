#include <openbeacon.h>

/* physical web beacon packet */
static const uint8_t g_beacon_pkt[] = {
	/* 0x03: Service List */
	 3,0x03, 0xD8, 0xFE,
	/* 0x16: Service Data - 'http://get.OpenBeacon.org' */
	21,0x16, 0xD8, 0xFE, 0x00, 0x20,
	   PROTO_HTTP,'g','e','t','.','O','p','e','n','B','e','a','c','o','n',DOT_ORG
};

void entry(void)
{
	int8_t tag_angle;

	/* set advertisment packet */
	radio_advertise(&g_beacon_pkt, sizeof(g_beacon_pkt));
	/* run advertisement in background every 995ms */
	radio_interval_ms(995);

	/* infinite foreground loop */
	while(TRUE)
	{
		/* get tag angle from 3D accelerometer */
		acc_magnitude(&tag_angle);
		/* only tx while beacon is worn (facing forward +/-45 degree) */
		radio_enable(abs(tag_angle)<45);

		/* blink once every three seconds */
		timer_wait_ms(3000);
		pin_set(CONFIG_LED_PIN);
		timer_wait_ms(1);
		pin_clear(CONFIG_LED_PIN);
	}
}
