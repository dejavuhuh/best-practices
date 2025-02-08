import { Stack } from '@mui/material'
import Button from '@mui/material/Button'
import { useMutation } from '@tanstack/react-query'
import wretch from 'wretch'

const api = wretch('/api').resolve(r => r.json())

export default function ButtonUsage() {
  const { mutate: mockHugeData, isPending } = useMutation({
    mutationFn: () => api.url('/mock-huge-data').post({
      size: 1000000,
    }),
  })

  return (
    <Stack direction="row" spacing={1}>
      <Button
        variant="contained"
        onClick={() => mockHugeData()}
        loading={isPending}
        loadingPosition="start"
      >
        生成数据
      </Button>
      <Button
        variant="contained"
        onClick={() => mockHugeData()}
        loading={isPending}
        loadingPosition="start"
      >
        异步导出
      </Button>
    </Stack>
  )
}
