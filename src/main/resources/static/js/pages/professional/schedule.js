import { CoreModule } from './schedule/core.js';
import { WorkScheduleModule } from './schedule/work-schedule.js';
import { BlocksModule } from './schedule/blocks.js';

window.professionalScheduleApp = {
    ...CoreModule,
    ...WorkScheduleModule,
    ...BlocksModule
};
