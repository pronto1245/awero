import { Body, Controller, Headers, Post } from '@nestjs/common';
import { CreateSupportTicketDto, SupportService } from './support.service';

@Controller('support')
export class SupportController {
  constructor(private readonly support: SupportService) {}

  @Post('diagnostics')
  submitDiagnostics(@Headers('authorization') authorization: string | undefined, @Body() body: CreateSupportTicketDto) {
    return this.support.submitDiagnostics(authorization, body);
  }
}
